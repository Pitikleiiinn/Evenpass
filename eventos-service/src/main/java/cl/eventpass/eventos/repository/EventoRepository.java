package cl.eventpass.eventos.repository;

import cl.eventpass.eventos.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    boolean existsByNombre(String nombre);

    List<Evento> findAllByOrderByFechaAsc();

    /**
     * Descuento ATOMICO de aforo: la condicion "aforoDisponible >= :cantidad" se evalua dentro del
     * mismo UPDATE, por lo que nunca se vende mas que la capacidad aunque haya miles de
     * solicitudes concurrentes. Devuelve 1 si descuenta, 0 si no hay cupos.
     */
    @Modifying(clearAutomatically = true)
    @Query("update Evento e set e.aforoDisponible = e.aforoDisponible - :cantidad "
            + "where e.id = :id and e.aforoDisponible >= :cantidad")
    int descontarAforo(@Param("id") Long id, @Param("cantidad") int cantidad);

    /** Devuelve cupos (compensacion) sin superar nunca el aforo total. */
    @Modifying(clearAutomatically = true)
    @Query("update Evento e set e.aforoDisponible = e.aforoDisponible + :cantidad "
            + "where e.id = :id and e.aforoDisponible + :cantidad <= e.aforoTotal")
    int devolverAforo(@Param("id") Long id, @Param("cantidad") int cantidad);
}
