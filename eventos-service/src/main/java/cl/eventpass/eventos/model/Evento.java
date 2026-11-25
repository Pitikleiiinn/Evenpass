package cl.eventpass.eventos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private String lugar;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private int aforoTotal;

    /** Se modifica SOLO mediante las consultas atomicas del repositorio. */
    @Column(nullable = false)
    private int aforoDisponible;

    @Column(nullable = false)
    private int precio;

    protected Evento() {
        // requerido por JPA
    }

    public Evento(String nombre, String lugar, LocalDateTime fecha, int aforoTotal, int precio) {
        this.nombre = nombre;
        this.lugar = lugar;
        this.fecha = fecha;
        this.aforoTotal = aforoTotal;
        this.aforoDisponible = aforoTotal;
        this.precio = precio;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getLugar() { return lugar; }
    public LocalDateTime getFecha() { return fecha; }
    public int getAforoTotal() { return aforoTotal; }
    public int getAforoDisponible() { return aforoDisponible; }
    public int getPrecio() { return precio; }
}
