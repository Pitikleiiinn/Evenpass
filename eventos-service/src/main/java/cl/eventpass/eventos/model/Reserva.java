package cl.eventpass.eventos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Cupos reservados para una orden. La clave primaria es el id de la orden, lo que hace la
 * reserva IDEMPOTENTE: si SQS entrega el mismo mensaje dos veces, el aforo se descuenta una sola vez.
 */
@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @Column(length = 64)
    private String ordenId;

    @Column(nullable = false)
    private Long eventoId;

    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false)
    private Instant creadaEn;

    protected Reserva() {
        // requerido por JPA
    }

    public Reserva(String ordenId, Long eventoId, int cantidad) {
        this.ordenId = ordenId;
        this.eventoId = eventoId;
        this.cantidad = cantidad;
        this.creadaEn = Instant.now();
    }

    public String getOrdenId() { return ordenId; }
    public Long getEventoId() { return eventoId; }
    public int getCantidad() { return cantidad; }
    public Instant getCreadaEn() { return creadaEn; }
}
