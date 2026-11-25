package cl.eventpass.auth.service;

import cl.eventpass.auth.dto.LoginRequest;
import cl.eventpass.auth.dto.RegistroRequest;
import cl.eventpass.auth.dto.TokenResponse;
import cl.eventpass.auth.dto.UsuarioResponse;
import cl.eventpass.auth.model.Rol;
import cl.eventpass.auth.model.Usuario;
import cl.eventpass.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private JwtEncoder jwtEncoder;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private AuthService servicio;

    @BeforeEach
    void setUp() {
        servicio = new AuthService(usuarios, encoder, jwtEncoder, 60);
    }

    @Test
    void registrar_creaCompradorConPasswordHasheada() {
        when(usuarios.existsByEmail("ana@mail.cl")).thenReturn(false);
        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resp = servicio.registrar(new RegistroRequest("Ana", " Ana@Mail.cl ", "clave1234"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertEquals("ana@mail.cl", guardado.getEmail());
        assertEquals(Rol.COMPRADOR, guardado.getRol());
        assertNotEquals("clave1234", guardado.getPasswordHash());
        assertTrue(encoder.matches("clave1234", guardado.getPasswordHash()));
        assertEquals("COMPRADOR", resp.rol());
    }

    @Test
    void registrar_emailDuplicado_lanza409() {
        when(usuarios.existsByEmail("ana@mail.cl")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(new RegistroRequest("Ana", "ana@mail.cl", "clave1234")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void login_credencialesValidas_devuelveTokenConRol() {
        Usuario u = new Usuario("Ana", "ana@mail.cl", encoder.encode("clave1234"), Rol.STAFF);
        when(usuarios.findByEmail("ana@mail.cl")).thenReturn(Optional.of(u));
        Jwt jwt = Jwt.withTokenValue("token-de-prueba").header("alg", "HS256").claim("sub", "1").build();
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        TokenResponse resp = servicio.login(new LoginRequest("ANA@mail.cl", "clave1234"));

        assertEquals("token-de-prueba", resp.token());
        assertEquals("Bearer", resp.tipo());
        assertEquals("STAFF", resp.rol());
        assertEquals(3600, resp.expiraEnSegundos());
    }

    @Test
    void login_passwordIncorrecta_lanza401() {
        Usuario u = new Usuario("Ana", "ana@mail.cl", encoder.encode("clave1234"), Rol.COMPRADOR);
        when(usuarios.findByEmail("ana@mail.cl")).thenReturn(Optional.of(u));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.login(new LoginRequest("ana@mail.cl", "otra-clave")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void login_usuarioInexistente_lanza401() {
        when(usuarios.findByEmail("nadie@mail.cl")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.login(new LoginRequest("nadie@mail.cl", "clave1234")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
