package cl.eventpass.eventos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReservaRequest(
        @NotBlank String ordenId,
        @NotNull Long eventoId,
        @Min(1) int cantidad) {
}
