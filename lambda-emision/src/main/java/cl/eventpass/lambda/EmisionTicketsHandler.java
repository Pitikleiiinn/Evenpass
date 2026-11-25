package cl.eventpass.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Funcion FaaS que consume la cola SQS de ordenes pendientes y las convierte en tickets:
 * <ol>
 *   <li>Lee la orden (si ya no esta PENDIENTE_EMISION, el mensaje es un duplicado y se ignora).</li>
 *   <li>Eventos: verifica y descuenta el aforo de forma atomica.</li>
 *   <li>Si NO hay cupos, la orden se RECHAZA y no se genera ticket.</li>
 *   <li>Tickets: genera el codigo de acceso.</li>
 *   <li>Ordenes: marca la orden como EMITIDA.</li>
 * </ol>
 * Si algo falla, solo ese mensaje se devuelve a la cola (ReportBatchItemFailures) y tras 5 intentos
 * pasa a la cola de mensajes muertos (DLQ). Todos los pasos son idempotentes.
 */
public class EmisionTicketsHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final Logger LOG = Logger.getLogger(EmisionTicketsHandler.class.getName());

    private final Servicios servicios;
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** Constructor que usa AWS Lambda: configuracion desde variables de entorno. */
    public EmisionTicketsHandler() {
        this(HttpServicios.desdeEntorno());
    }

    public EmisionTicketsHandler(Servicios servicios) {
        this.servicios = servicios;
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent evento, Context contexto) {
        List<SQSBatchResponse.BatchItemFailure> fallidos = new ArrayList<>();
        List<SQSEvent.SQSMessage> registros = evento.getRecords() == null ? List.of() : evento.getRecords();

        for (SQSEvent.SQSMessage registro : registros) {
            try {
                procesar(registro.getBody());
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Fallo al procesar el mensaje " + registro.getMessageId()
                        + "; volvera a la cola", e);
                fallidos.add(new SQSBatchResponse.BatchItemFailure(registro.getMessageId()));
            }
        }
        return new SQSBatchResponse(fallidos);
    }

    void procesar(String cuerpo) throws Exception {
        OrdenMensaje orden = mapper.readValue(cuerpo, OrdenMensaje.class);
        LOG.info("Procesando orden " + orden.ordenId());

        if (!"PENDIENTE_EMISION".equals(servicios.obtenerEstadoOrden(orden.ordenId()))) {
            LOG.info("Orden " + orden.ordenId() + " ya procesada; se ignora el mensaje duplicado");
            return;
        }

        ResultadoReserva reserva = servicios.reservarAforo(orden);
        switch (reserva) {
            case SIN_CUPO -> {
                servicios.actualizarOrden(orden.ordenId(), "RECHAZADA", "Sin cupos disponibles para el evento");
                LOG.info("Orden " + orden.ordenId() + " rechazada: sin cupos");
            }
            case EVENTO_NO_EXISTE -> {
                servicios.actualizarOrden(orden.ordenId(), "RECHAZADA", "El evento no existe");
                LOG.info("Orden " + orden.ordenId() + " rechazada: evento inexistente");
            }
            case RESERVADO -> {
                try {
                    servicios.emitirTickets(orden);
                } catch (RuntimeException e) {
                    // Compensacion: no dejar cupos retenidos si no se pudo generar el ticket
                    servicios.liberarAforo(orden.ordenId());
                    throw e;
                }
                servicios.actualizarOrden(orden.ordenId(), "EMITIDA", null);
                LOG.info("Orden " + orden.ordenId() + " emitida");
            }
        }
    }
}
