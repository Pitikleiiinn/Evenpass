package cl.eventpass.eventos.controller;

import cl.eventpass.eventos.dto.EventoRequest;
import cl.eventpass.eventos.dto.EventoResponse;
import cl.eventpass.eventos.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/eventos")
public class EventoController {

    private final EventoService servicio;

    public EventoController(EventoService servicio) {
        this.servicio = servicio;
    }

    /** COMPRADOR y STAFF: listado de eventos con aforo disponible. */
    @GetMapping
    public List<EventoResponse> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public EventoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    /** Solo STAFF: gestion del listado de eventos. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventoResponse crear(@Valid @RequestBody EventoRequest req) {
        return servicio.crear(req);
    }
}
