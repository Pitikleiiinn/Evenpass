package cl.eventpass.eventos.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record EventoRequest(
        @NotBlank String nombre,
        @NotBlank String lugar,
        @NotNull @Future LocalDateTime fecha,
        @Min(1) int aforo,
        @Min(0) int precio) {
}
