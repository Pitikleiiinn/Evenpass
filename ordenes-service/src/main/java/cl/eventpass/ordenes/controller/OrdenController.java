package cl.eventpass.ordenes.controller;

import cl.eventpass.ordenes.dto.OrdenRequest;
import cl.eventpass.ordenes.dto.OrdenResponse;
import cl.eventpass.ordenes.service.OrdenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ordenes")
public class OrdenController {

    private final OrdenService servicio;

    public OrdenController(OrdenService servicio) {
        this.servicio = servicio;
    }

    /** 202 Accepted: la orden queda aceptada y pendiente de emision (se procesa de forma asincrona). */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrdenResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrdenRequest req) {
        return servicio.crear(jwt.getSubject(), req);
    }

    @GetMapping("/mis-ordenes")
    public List<OrdenResponse> misOrdenes(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listarDe(jwt.getSubject());
    }

    /** Permite al comprador consultar si su orden ya fue emitida o rechazada. */
    @GetMapping("/{id}")
    public OrdenResponse obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return servicio.obtenerDe(id, jwt.getSubject());
    }
}
