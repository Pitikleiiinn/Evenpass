package cl.eventpass.auth.config;

import cl.eventpass.auth.model.Rol;
import cl.eventpass.auth.model.Usuario;
import cl.eventpass.auth.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Crea un comprador y un staff de demostracion (el staff no puede autoregistrarse). */
@Component
public class DatosSemilla implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String passComprador;
    private final String passStaff;

    public DatosSemilla(UsuarioRepository usuarios,
                        PasswordEncoder encoder,
                        @Value("${eventpass.seed.comprador-password}") String passComprador,
                        @Value("${eventpass.seed.staff-password}") String passStaff) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.passComprador = passComprador;
        this.passStaff = passStaff;
    }

    @Override
    public void run(String... args) {
        crear("Comprador Demo", "comprador@eventpass.cl", passComprador, Rol.COMPRADOR);
        crear("Staff Puerta", "staff@eventpass.cl", passStaff, Rol.STAFF);
    }

    private void crear(String nombre, String email, String password, Rol rol) {
        if (usuarios.existsByEmail(email)) {
            return;
        }
        try {
            usuarios.save(new Usuario(nombre, email, encoder.encode(password), rol));
        } catch (DataIntegrityViolationException e) {
            // Otra replica del servicio (Swarm) lo creo primero: es normal.
        }
    }
}
