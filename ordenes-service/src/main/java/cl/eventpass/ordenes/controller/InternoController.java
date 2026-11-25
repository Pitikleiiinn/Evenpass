package cl.eventpass.ordenes.controller;

import cl.eventpass.ordenes.dto.EstadoRequest;
import cl.eventpass.ordenes.dto.OrdenResponse;
import cl.eventpass.ordenes.service.OrdenService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints internos para la funcion Lambda (X-Internal-Key). No se exponen en el API Gateway. */
@RestController
@RequestMapping("/internal/ordenes")
public class InternoController {

    private final OrdenService servicio;

    public InternoController(OrdenService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{id}")
    public OrdenResponse obtener(@PathVariable String id) {
        return servicio.obtener(id);
    }

    @PatchMapping("/{id}/estado")
    public OrdenResponse actualizarEstado(@PathVariable String id, @Valid @RequestBody EstadoRequest req) {
        return servicio.actualizarEstado(id, req.estado(), req.motivo());
    }
}
