package cl.eventpass.lambda;

import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmisionTicketsHandlerTest {

    private static final String MENSAJE =
            "{\"ordenId\":\"o-1\",\"eventoId\":1,\"compradorId\":\"7\",\"cantidad\":2}";

    /** Doble de prueba: registra las llamadas y permite simular respuestas/fallas. */
    static class ServiciosFalsos implements Servicios {
        final List<String> llamadas = new ArrayList<>();
        String estadoOrden = "PENDIENTE_EMISION";
        ResultadoReserva reserva = ResultadoReserva.RESERVADO;
        boolean fallaEmision = false;

        @Override
        public String obtenerEstadoOrden(String ordenId) {
            llamadas.add("obtener");
            return estadoOrden;
        }

        @Override
        public ResultadoReserva reservarAforo(OrdenMensaje orden) {
            llamadas.add("reservar");
            return reserva;
        }

        @Override
        public void liberarAforo(String ordenId) {
            llamadas.add("liberar");
        }

        @Override
        public void emitirTickets(OrdenMensaje orden) {
            llamadas.add("emitir");
            if (fallaEmision) {
                throw new ServicioException("tickets-service caido");
            }
        }

        @Override
        public void actualizarOrden(String ordenId, String estado, String motivo) {
            llamadas.add("actualizar:" + estado);
        }
    }

    private ServiciosFalsos servicios;
    private EmisionTicketsHandler handler;

    @BeforeEach
    void setUp() {
        servicios = new ServiciosFalsos();
        handler = new EmisionTicketsHandler(servicios);
    }

    private SQSEvent evento(String messageId, String cuerpo) {
        SQSEvent.SQSMessage m = new SQSEvent.SQSMessage();
        m.setMessageId(messageId);
        m.setBody(cuerpo);
        SQSEvent e = new SQSEvent();
        e.setRecords(List.of(m));
        return e;
    }

    @Test
    void ordenConCupo_emiteTicketsYMarcaLaOrdenEmitida() {
        SQSBatchResponse resp = handler.handleRequest(evento("m-1", MENSAJE), null);

        assertTrue(resp.getBatchItemFailures().isEmpty());
        assertEquals(List.of("obtener", "reservar", "emitir", "actualizar:EMITIDA"), servicios.llamadas);
    }

    @Test
    void eventoSinCupos_rechazaLaOrdenYNoGeneraTicket() {
        servicios.reserva = ResultadoReserva.SIN_CUPO;

        SQSBatchResponse resp = handler.handleRequest(evento("m-2", MENSAJE), null);

        assertTrue(resp.getBatchItemFailures().isEmpty());
        assertEquals(List.of("obtener", "reservar", "actualizar:RECHAZADA"), servicios.llamadas);
    }

    @Test
    void eventoInexistente_rechazaLaOrden() {
        servicios.reserva = ResultadoReserva.EVENTO_NO_EXISTE;

        handler.handleRequest(evento("m-3", MENSAJE), null);

        assertEquals(List.of("obtener", "reservar", "actualizar:RECHAZADA"), servicios.llamadas);
    }

    @Test
    void mensajeDuplicado_ordenYaProcesada_seIgnora() {
        servicios.estadoOrden = "EMITIDA";

        SQSBatchResponse resp = handler.handleRequest(evento("m-4", MENSAJE), null);

        assertTrue(resp.getBatchItemFailures().isEmpty());
        assertEquals(List.of("obtener"), servicios.llamadas);
    }

    @Test
    void fallaAlEmitir_liberaElAforoYDevuelveElMensajeALaCola() {
        servicios.fallaEmision = true;

        SQSBatchResponse resp = handler.handleRequest(evento("m-5", MENSAJE), null);

        assertEquals(1, resp.getBatchItemFailures().size());
        assertEquals("m-5", resp.getBatchItemFailures().get(0).getItemIdentifier());
        assertEquals(List.of("obtener", "reservar", "emitir", "liberar"), servicios.llamadas);
    }

    @Test
    void mensajeInvalido_sereportaComoFallo() {
        SQSBatchResponse resp = handler.handleRequest(evento("m-6", "esto no es json"), null);

        assertEquals(1, resp.getBatchItemFailures().size());
        assertEquals("m-6", resp.getBatchItemFailures().get(0).getItemIdentifier());
    }

    @Test
    void lote_unMensajeFallaYElOtroSeProcesa() {
        SQSEvent.SQSMessage bueno = new SQSEvent.SQSMessage();
        bueno.setMessageId("ok");
        bueno.setBody(MENSAJE);
        SQSEvent.SQSMessage malo = new SQSEvent.SQSMessage();
        malo.setMessageId("malo");
        malo.setBody("{{");
        SQSEvent lote = new SQSEvent();
        lote.setRecords(List.of(malo, bueno));

        SQSBatchResponse resp = handler.handleRequest(lote, null);

        assertEquals(1, resp.getBatchItemFailures().size());
        assertEquals("malo", resp.getBatchItemFailures().get(0).getItemIdentifier());
        assertTrue(servicios.llamadas.contains("actualizar:EMITIDA"));
    }
}
