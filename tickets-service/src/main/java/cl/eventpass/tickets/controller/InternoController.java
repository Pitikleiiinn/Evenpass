package cl.eventpass.tickets.controller;

import cl.eventpass.tickets.dto.EmitirRequest;
import cl.eventpass.tickets.dto.TicketResponse;
import cl.eventpass.tickets.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoint interno para la funcion Lambda (X-Internal-Key). No se expone en el API Gateway. */
@RestController
@RequestMapping("/internal/tickets")
public class InternoController {

    private final TicketService servicio;

    public InternoController(TicketService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<TicketResponse> emitir(@Valid @RequestBody EmitirRequest req) {
        return servicio.emitir(req);
    }
}
