package cl.eventpass.auth.service;

import cl.eventpass.auth.dto.LoginRequest;
import cl.eventpass.auth.dto.RegistroRequest;
import cl.eventpass.auth.dto.TokenResponse;
import cl.eventpass.auth.dto.UsuarioResponse;
import cl.eventpass.auth.model.Rol;
import cl.eventpass.auth.model.Usuario;
import cl.eventpass.auth.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long ttlMinutos;

    public AuthService(UsuarioRepository usuarios,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder,
                       @Value("${eventpass.security.token-ttl-minutos:60}") long ttlMinutos) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.ttlMinutos = ttlMinutos;
    }

    /** El autoregistro siempre crea COMPRADORES; el staff se provisiona aparte. */
    public UsuarioResponse registrar(RegistroRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarios.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya esta registrado");
        }
        Usuario nuevo = new Usuario(req.nombre().trim(), email, passwordEncoder.encode(req.password()), Rol.COMPRADOR);
        return UsuarioResponse.de(usuarios.save(nuevo));
    }

    public TokenResponse login(LoginRequest req) {
        Usuario u = usuarios.findByEmail(req.email().trim().toLowerCase())
                .filter(x -> passwordEncoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas"));
        return new TokenResponse(generarToken(u), "Bearer", Duration.ofMinutes(ttlMinutos).toSeconds(), u.getRol().name());
    }

    public UsuarioResponse obtener(Long id) {
        return usuarios.findById(id)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private String generarToken(Usuario u) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("eventpass-auth")
                .subject(String.valueOf(u.getId()))
                .issuedAt(ahora)
                .expiresAt(ahora.plus(Duration.ofMinutes(ttlMinutos)))
                .claim("email", u.getEmail())
                .claim("rol", u.getRol().name())
                .build();
        return jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
