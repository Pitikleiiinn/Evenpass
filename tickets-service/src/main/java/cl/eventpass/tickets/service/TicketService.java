package cl.eventpass.tickets.service;

import cl.eventpass.tickets.dto.EmitirRequest;
import cl.eventpass.tickets.dto.TicketResponse;
import cl.eventpass.tickets.dto.ValidacionResponse;
import cl.eventpass.tickets.model.Ticket;
import cl.eventpass.tickets.repository.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository tickets;
    private final CodigoGenerador generador;

    public TicketService(TicketRepository tickets, CodigoGenerador generador) {
        this.tickets = tickets;
        this.generador = generador;
    }

    /**
     * Genera los codigos de acceso de una orden (llamado por la Lambda tras reservar el aforo).
     * Idempotente: si la orden ya tiene tickets, devuelve los existentes sin crear nuevos.
     */
    @Transactional
    public List<TicketResponse> emitir(EmitirRequest req) {
        List<Ticket> existentes = tickets.findByOrdenIdOrderByNumero(req.ordenId());
        if (!existentes.isEmpty()) {
            return existentes.stream().map(TicketResponse::de).toList();
        }
        List<Ticket> nuevos = new ArrayList<>();
        for (int numero = 1; numero <= req.cantidad(); numero++) {
            nuevos.add(new Ticket(generador.generar(), req.ordenId(), numero, req.eventoId(), req.compradorId()));
        }
        return tickets.saveAll(nuevos).stream().map(TicketResponse::de).toList();
    }

    /** Codigos de acceso de los tickets ya emitidos al comprador. */
    @Transactional(readOnly = true)
    public List<TicketResponse> listarDe(String compradorId) {
        return tickets.findByCompradorIdOrderByEmitidoEnDesc(compradorId).stream().map(TicketResponse::de).toList();
    }

    /**
     * El staff valida un codigo en la puerta. Un codigo utilizado correctamente NO puede volver a usarse:
     * 200 = ingreso autorizado (queda USADO), 409 = ya fue utilizado, 404 = el codigo no existe.
     */
    @Transactional
    public ValidacionResponse validar(String codigoIngresado) {
        String codigo = codigoIngresado.trim().toUpperCase();
        if (tickets.marcarUsado(codigo, Instant.now()) == 1) {
            Ticket t = tickets.findByCodigo(codigo).orElseThrow();
            return new ValidacionResponse(true, t.getCodigo(), t.getEventoId(), "Ingreso autorizado");
        }
        if (tickets.findByCodigo(codigo).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El codigo ya fue utilizado");
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Codigo de acceso inexistente");
    }
}
