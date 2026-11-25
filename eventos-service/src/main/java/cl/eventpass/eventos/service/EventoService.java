package cl.eventpass.eventos.service;

import cl.eventpass.eventos.dto.EventoRequest;
import cl.eventpass.eventos.dto.EventoResponse;
import cl.eventpass.eventos.model.Evento;
import cl.eventpass.eventos.model.Reserva;
import cl.eventpass.eventos.repository.EventoRepository;
import cl.eventpass.eventos.repository.ReservaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EventoService {

    private final EventoRepository eventos;
    private final ReservaRepository reservas;

    public EventoService(EventoRepository eventos, ReservaRepository reservas) {
        this.eventos = eventos;
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> listar() {
        return eventos.findAllByOrderByFechaAsc().stream().map(EventoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public EventoResponse obtener(Long id) {
        return eventos.findById(id)
                .map(EventoResponse::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
    }

    @Transactional
    public EventoResponse crear(EventoRequest req) {
        if (eventos.existsByNombre(req.nombre().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un evento con ese nombre");
        }
        Evento nuevo = new Evento(req.nombre().trim(), req.lugar().trim(), req.fecha(), req.aforo(), req.precio());
        return EventoResponse.de(eventos.save(nuevo));
    }

    /**
     * Verifica y descuenta el aforo para una orden (llamado por la funcion Lambda).
     * Es idempotente por ordenId y el descuento es atomico, asi que nunca se supera la capacidad.
     */
    @Transactional
    public ResultadoReserva reservar(Long eventoId, String ordenId, int cantidad) {
        if (!eventos.existsById(eventoId)) {
            return ResultadoReserva.EVENTO_NO_EXISTE;
        }
        if (reservas.existsById(ordenId)) {
            return ResultadoReserva.RESERVADO; // reintento de SQS: ya estaba reservado
        }
        if (eventos.descontarAforo(eventoId, cantidad) == 0) {
            return ResultadoReserva.SIN_CUPO;
        }
        reservas.save(new Reserva(ordenId, eventoId, cantidad));
        return ResultadoReserva.RESERVADO;
    }

    /** Compensacion: si despues de reservar falla la emision del ticket, se devuelven los cupos. */
    @Transactional
    public boolean liberar(String ordenId) {
        return reservas.findById(ordenId).map(r -> {
            eventos.devolverAforo(r.getEventoId(), r.getCantidad());
            reservas.delete(r);
            return true;
        }).orElse(false);
    }
}
