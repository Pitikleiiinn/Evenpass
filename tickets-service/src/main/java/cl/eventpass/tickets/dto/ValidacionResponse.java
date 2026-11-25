package cl.eventpass.tickets.dto;

public record ValidacionResponse(boolean valido, String codigo, Long eventoId, String mensaje) {
}
