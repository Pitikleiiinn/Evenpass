package cl.eventpass.tickets.dto;

import cl.eventpass.tickets.model.Ticket;

import java.time.Instant;

public record TicketResponse(Long id, String codigo, String ordenId, Long eventoId, String estado,
                             Instant emitidoEn, Instant usadoEn) {
    public static TicketResponse de(Ticket t) {
        return new TicketResponse(t.getId(), t.getCodigo(), t.getOrdenId(), t.getEventoId(),
                t.getEstado().name(), t.getEmitidoEn(), t.getUsadoEn());
    }
}
