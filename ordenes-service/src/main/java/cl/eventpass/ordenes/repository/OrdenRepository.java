package cl.eventpass.ordenes.repository;

import cl.eventpass.ordenes.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenRepository extends JpaRepository<Orden, String> {
    List<Orden> findByCompradorIdOrderByCreadaEnDesc(String compradorId);
}
