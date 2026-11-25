package cl.eventpass.ordenes.dto;

import cl.eventpass.ordenes.model.Orden;

import java.time.Instant;

public record OrdenResponse(String id, Long eventoId, int cantidad, String estado, String motivo,
                            Instant creadaEn, Instant actualizadaEn) {
    public static OrdenResponse de(Orden o) {
        return new OrdenResponse(o.getId(), o.getEventoId(), o.getCantidad(), o.getEstado().name(),
                o.getMotivo(), o.getCreadaEn(), o.getActualizadaEn());
    }
}
