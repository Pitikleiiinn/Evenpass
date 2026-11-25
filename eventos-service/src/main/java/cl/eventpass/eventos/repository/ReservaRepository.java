package cl.eventpass.eventos.repository;

import cl.eventpass.eventos.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, String> {
}
