package cl.eventpass.auth.dto;

import cl.eventpass.auth.model.Usuario;

public record UsuarioResponse(Long id, String nombre, String email, String rol) {
    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol().name());
    }
}
