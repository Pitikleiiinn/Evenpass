package cl.eventpass.ordenes.service;

import cl.eventpass.ordenes.dto.OrdenRequest;
import cl.eventpass.ordenes.dto.OrdenResponse;
import cl.eventpass.ordenes.messaging.OrdenMensaje;
import cl.eventpass.ordenes.messaging.OrdenPublisher;
import cl.eventpass.ordenes.model.EstadoOrden;
import cl.eventpass.ordenes.model.Orden;
import cl.eventpass.ordenes.repository.OrdenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class OrdenService {

    private static final Logger log = LoggerFactory.getLogger(OrdenService.class);

    private final OrdenRepository ordenes;
    private final OrdenPublisher publisher;

    public OrdenService(OrdenRepository ordenes, OrdenPublisher publisher) {
        this.ordenes = ordenes;
        this.publisher = publisher;
    }

    /**
     * Acepta la compra de inmediato: registra la orden como PENDIENTE_EMISION y la encola.
     * NO genera codigos ni descuenta aforo (eso lo hace la Lambda de forma asincrona).
     * El guardado se confirma ANTES de publicar, para que la Lambda siempre encuentre la orden.
     */
    public OrdenResponse crear(String compradorId, OrdenRequest req) {
        Orden orden = ordenes.save(new Orden(UUID.randomUUID().toString(), req.eventoId(), compradorId, req.cantidad()));
        try {
            publisher.publicar(new OrdenMensaje(orden.getId(), orden.getEventoId(), compradorId, orden.getCantidad()));
        } catch (RuntimeException e) {
            log.error("No se pudo encolar la orden {}", orden.getId(), e);
            orden.cambiarEstado(EstadoOrden.RECHAZADA, "No se pudo encolar la orden para su emision");
            ordenes.save(orden);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo procesar la orden en este momento, intenta nuevamente");
        }
        return OrdenResponse.de(orden);
    }

    public List<OrdenResponse> listarDe(String compradorId) {
        return ordenes.findByCompradorIdOrderByCreadaEnDesc(compradorId).stream().map(OrdenResponse::de).toList();
    }

    /** Un comprador solo puede ver sus propias ordenes. */
    public OrdenResponse obtenerDe(String ordenId, String compradorId) {
        return ordenes.findById(ordenId)
                .filter(o -> o.getCompradorId().equals(compradorId))
                .map(OrdenResponse::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada"));
    }

    // ---- uso interno (Lambda) ----

    public OrdenResponse obtener(String ordenId) {
        return ordenes.findById(ordenId)
                .map(OrdenResponse::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada"));
    }

    /** Idempotente: repetir el mismo estado no falla; salir de un estado final si. */
    public OrdenResponse actualizarEstado(String ordenId, EstadoOrden nuevo, String motivo) {
        Orden orden = ordenes.findById(ordenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada"));
        if (orden.getEstado() == nuevo) {
            return OrdenResponse.de(orden);
        }
        if (orden.getEstado() != EstadoOrden.PENDIENTE_EMISION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La orden ya fue procesada con estado " + orden.getEstado());
        }
        orden.cambiarEstado(nuevo, motivo);
        return OrdenResponse.de(ordenes.save(orden));
    }
}
