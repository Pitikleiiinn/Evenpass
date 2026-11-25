package cl.eventpass.tickets.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmitirRequest(
        @NotBlank String ordenId,
        @NotNull Long eventoId,
        @NotBlank String compradorId,
        @Min(1) @Max(6) int cantidad) {
}
