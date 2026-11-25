package cl.eventpass.auth.dto;

public record TokenResponse(String token, String tipo, long expiraEnSegundos, String rol) {
}
