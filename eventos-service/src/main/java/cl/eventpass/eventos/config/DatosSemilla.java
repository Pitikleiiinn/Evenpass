package cl.eventpass.eventos.config;

import cl.eventpass.eventos.model.Evento;
import cl.eventpass.eventos.repository.EventoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Eventos de demostracion para poder probar el flujo de compra desde el primer arranque. */
@Component
public class DatosSemilla implements CommandLineRunner {

    private final EventoRepository eventos;

    public DatosSemilla(EventoRepository eventos) {
        this.eventos = eventos;
    }

    @Override
    public void run(String... args) {
        LocalDateTime base = LocalDateTime.now().plusMonths(2).withHour(20).withMinute(0).withSecond(0).withNano(0);
        crear(new Evento("Concierto de Preventa Duoc Fest", "Gimnasio Sede Plaza Norte", base, 200, 15000));
        crear(new Evento("Noche de Bandas Universitarias", "Teatro Universitario", base.plusDays(7), 120, 8000));
        crear(new Evento("Tocata Aforo Reducido (demo de agotado)", "Sala Mini", base.plusDays(14), 5, 5000));
    }

    private void crear(Evento e) {
        if (eventos.existsByNombre(e.getNombre())) {
            return;
        }
        try {
            eventos.save(e);
        } catch (DataIntegrityViolationException ex) {
            // Otra replica (Swarm) ya lo inserto.
        }
    }
}
