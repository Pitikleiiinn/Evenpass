package cl.eventpass.tickets.controller;

import cl.eventpass.tickets.dto.TicketResponse;
import cl.eventpass.tickets.dto.ValidacionResponse;
import cl.eventpass.tickets.dto.ValidarRequest;
import cl.eventpass.tickets.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService servicio;

    public TicketController(TicketService servicio) {
        this.servicio = servicio;
    }

    /** COMPRADOR: consulta el codigo de acceso de sus tickets emitidos. */
    @GetMapping("/mis-tickets")
    public List<TicketResponse> misTickets(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listarDe(jwt.getSubject());
    }

    /** STAFF: valida el codigo en la puerta y registra su utilizacion. */
    @PostMapping("/validar")
    public ValidacionResponse validar(@Valid @RequestBody ValidarRequest req) {
        return servicio.validar(req.codigo());
    }
}
