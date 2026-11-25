package cl.eventpass.tickets.repository;

import cl.eventpass.tickets.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByOrdenIdOrderByNumero(String ordenId);

    List<Ticket> findByCompradorIdOrderByEmitidoEnDesc(String compradorId);

    Optional<Ticket> findByCodigo(String codigo);

    /**
     * Marca el ticket como USADO SOLO si sigue EMITIDO. Al ser un unico UPDATE condicional, dos
     * validaciones simultaneas del mismo codigo no pueden tener exito las dos: una devuelve 1 y la otra 0.
     */
    @Modifying(clearAutomatically = true)
    @Query("update Ticket t set t.estado = cl.eventpass.tickets.model.EstadoTicket.USADO, t.usadoEn = :ahora "
            + "where t.codigo = :codigo and t.estado = cl.eventpass.tickets.model.EstadoTicket.EMITIDO")
    int marcarUsado(@Param("codigo") String codigo, @Param("ahora") Instant ahora);
}
