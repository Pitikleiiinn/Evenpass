package cl.eventpass.ordenes.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrdenRequest(
        @NotNull Long eventoId,
        @Min(1) @Max(6) int cantidad) {
}
