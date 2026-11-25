package cl.eventpass.ordenes.dto;

import cl.eventpass.ordenes.model.EstadoOrden;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de PATCH /internal/ordenes/{id}/estado (lo usa la funcion Lambda). */
public record EstadoRequest(@NotNull EstadoOrden estado, String motivo) {
}
