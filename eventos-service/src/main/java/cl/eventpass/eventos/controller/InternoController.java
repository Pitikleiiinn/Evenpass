package cl.eventpass.eventos.controller;

import cl.eventpass.eventos.dto.ReservaRequest;
import cl.eventpass.eventos.service.EventoService;
import cl.eventpass.eventos.service.ResultadoReserva;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints internos usados SOLO por la funcion Lambda (cabecera X-Internal-Key).
 * No se publican en el API Gateway.
 */
@RestController
@RequestMapping("/internal/reservas")
public class InternoController {

    private final EventoService servicio;

    public InternoController(EventoService servicio) {
        this.servicio = servicio;
    }

    /** 200 = reservado, 409 = sin cupos, 404 = el evento no existe. */
    @PostMapping
    public ResponseEntity<Map<String, String>> reservar(@Valid @RequestBody ReservaRequest req) {
        ResultadoReserva resultado = servicio.reservar(req.eventoId(), req.ordenId(), req.cantidad());
        Map<String, String> cuerpo = Map.of("resultado", resultado.name());
        return switch (resultado) {
            case RESERVADO -> ResponseEntity.ok(cuerpo);
            case SIN_CUPO -> ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpo);
            case EVENTO_NO_EXISTE -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpo);
        };
    }

    @DeleteMapping("/{ordenId}")
    public ResponseEntity<Void> liberar(@PathVariable String ordenId) {
        return servicio.liberar(ordenId) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
