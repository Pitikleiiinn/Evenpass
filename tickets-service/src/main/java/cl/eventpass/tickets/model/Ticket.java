package cl.eventpass.tickets.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "tickets",
        indexes = @Index(name = "idx_tickets_comprador", columnList = "comprador_id"),
        // (ordenId, numero) unico: emitir dos veces la misma orden nunca duplica tickets
        uniqueConstraints = @UniqueConstraint(name = "uq_ticket_orden_numero", columnNames = {"orden_id", "numero"}))
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String codigo;

    @Column(name = "orden_id", nullable = false, length = 64)
    private String ordenId;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false)
    private Long eventoId;

    @Column(name = "comprador_id", nullable = false)
    private String compradorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTicket estado;

    @Column(nullable = false)
    private Instant emitidoEn;

    private Instant usadoEn;

    protected Ticket() {
        // requerido por JPA
    }

    public Ticket(String codigo, String ordenId, int numero, Long eventoId, String compradorId) {
        this.codigo = codigo;
        this.ordenId = ordenId;
        this.numero = numero;
        this.eventoId = eventoId;
        this.compradorId = compradorId;
        this.estado = EstadoTicket.EMITIDO;
        this.emitidoEn = Instant.now();
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getOrdenId() { return ordenId; }
    public int getNumero() { return numero; }
    public Long getEventoId() { return eventoId; }
    public String getCompradorId() { return compradorId; }
    public EstadoTicket getEstado() { return estado; }
    public Instant getEmitidoEn() { return emitidoEn; }
    public Instant getUsadoEn() { return usadoEn; }
}
