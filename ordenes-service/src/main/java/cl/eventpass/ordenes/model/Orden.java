package cl.eventpass.ordenes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "ordenes")
public class Orden {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private Long eventoId;

    @Column(nullable = false)
    private String compradorId;

    @Column(nullable = false)
    private int cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrden estado;

    private String motivo;

    @Column(nullable = false)
    private Instant creadaEn;

    @Column(nullable = false)
    private Instant actualizadaEn;

    protected Orden() {
        // requerido por JPA
    }

    /** Toda orden nace aceptada y pendiente de emision (no se toca el aforo en la compra). */
    public Orden(String id, Long eventoId, String compradorId, int cantidad) {
        this.id = id;
        this.eventoId = eventoId;
        this.compradorId = compradorId;
        this.cantidad = cantidad;
        this.estado = EstadoOrden.PENDIENTE_EMISION;
        this.creadaEn = Instant.now();
        this.actualizadaEn = this.creadaEn;
    }

    public void cambiarEstado(EstadoOrden nuevo, String motivo) {
        this.estado = nuevo;
        this.motivo = motivo;
        this.actualizadaEn = Instant.now();
    }

    public String getId() { return id; }
    public Long getEventoId() { return eventoId; }
    public String getCompradorId() { return compradorId; }
    public int getCantidad() { return cantidad; }
    public EstadoOrden getEstado() { return estado; }
    public String getMotivo() { return motivo; }
    public Instant getCreadaEn() { return creadaEn; }
    public Instant getActualizadaEn() { return actualizadaEn; }
}
