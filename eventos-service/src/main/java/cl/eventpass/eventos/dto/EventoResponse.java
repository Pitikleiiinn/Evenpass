package cl.eventpass.eventos.dto;

import cl.eventpass.eventos.model.Evento;

import java.time.LocalDateTime;

public record EventoResponse(Long id, String nombre, String lugar, LocalDateTime fecha,
                             int aforoTotal, int aforoDisponible, int precio) {
    public static EventoResponse de(Evento e) {
        return new EventoResponse(e.getId(), e.getNombre(), e.getLugar(), e.getFecha(),
                e.getAforoTotal(), e.getAforoDisponible(), e.getPrecio());
    }
}
