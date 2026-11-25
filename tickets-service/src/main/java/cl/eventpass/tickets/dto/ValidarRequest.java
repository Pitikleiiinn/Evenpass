package cl.eventpass.tickets.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidarRequest(@NotBlank String codigo) {
}
