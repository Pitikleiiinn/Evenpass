package cl.eventpass.eventos.service;

import cl.eventpass.eventos.dto.EventoRequest;
import cl.eventpass.eventos.model.Evento;
import cl.eventpass.eventos.model.Reserva;
import cl.eventpass.eventos.repository.EventoRepository;
import cl.eventpass.eventos.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock
    private EventoRepository eventos;
    @Mock
    private ReservaRepository reservas;

    private EventoService servicio;

    @BeforeEach
    void setUp() {
        servicio = new EventoService(eventos, reservas);
    }

    @Test
    void reservar_conCupo_descuentaAforoYGuardaReserva() {
        when(eventos.existsById(1L)).thenReturn(true);
        when(reservas.existsById("orden-1")).thenReturn(false);
        when(eventos.descontarAforo(1L, 2)).thenReturn(1);

        ResultadoReserva resultado = servicio.reservar(1L, "orden-1", 2);

        assertEquals(ResultadoReserva.RESERVADO, resultado);
        verify(reservas).save(any(Reserva.class));
    }

    @Test
    void reservar_sinCupo_noGuardaReserva() {
        when(eventos.existsById(1L)).thenReturn(true);
        when(reservas.existsById("orden-2")).thenReturn(false);
        when(eventos.descontarAforo(1L, 3)).thenReturn(0);

        ResultadoReserva resultado = servicio.reservar(1L, "orden-2", 3);

        assertEquals(ResultadoReserva.SIN_CUPO, resultado);
        verify(reservas, never()).save(any(Reserva.class));
    }

    @Test
    void reservar_ordenYaReservada_esIdempotenteYNoDescuentaDeNuevo() {
        when(eventos.existsById(1L)).thenReturn(true);
        when(reservas.existsById("orden-3")).thenReturn(true);

        ResultadoReserva resultado = servicio.reservar(1L, "orden-3", 1);

        assertEquals(ResultadoReserva.RESERVADO, resultado);
        verify(eventos, never()).descontarAforo(anyLong(), anyInt());
    }

    @Test
    void reservar_eventoInexistente_informaQueNoExiste() {
        when(eventos.existsById(99L)).thenReturn(false);

        assertEquals(ResultadoReserva.EVENTO_NO_EXISTE, servicio.reservar(99L, "orden-4", 1));
        verify(eventos, never()).descontarAforo(anyLong(), anyInt());
    }

    @Test
    void liberar_reservaExistente_devuelveAforoYBorraReserva() {
        Reserva reserva = new Reserva("orden-5", 1L, 2);
        when(reservas.findById("orden-5")).thenReturn(Optional.of(reserva));

        assertTrue(servicio.liberar("orden-5"));

        verify(eventos).devolverAforo(1L, 2);
        verify(reservas).delete(reserva);
    }

    @Test
    void liberar_sinReserva_noHaceNada() {
        when(reservas.findById("orden-6")).thenReturn(Optional.empty());

        assertFalse(servicio.liberar("orden-6"));
        verify(eventos, never()).devolverAforo(anyLong(), anyInt());
    }

    @Test
    void crear_nombreDuplicado_lanza409() {
        when(eventos.existsByNombre("Rock Fest")).thenReturn(true);
        EventoRequest req = new EventoRequest("Rock Fest", "Estadio", LocalDateTime.now().plusDays(10), 100, 5000);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> servicio.crear(req));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(eventos, never()).save(any(Evento.class));
    }

    @Test
    void crear_eventoNuevo_iniciaConAforoDisponibleIgualAlTotal() {
        when(eventos.existsByNombre("Rock Fest")).thenReturn(false);
        when(eventos.save(any(Evento.class))).thenAnswer(inv -> inv.getArgument(0));
        EventoRequest req = new EventoRequest("Rock Fest", "Estadio", LocalDateTime.now().plusDays(10), 100, 5000);

        var resp = servicio.crear(req);

        assertEquals(100, resp.aforoTotal());
        assertEquals(100, resp.aforoDisponible());
    }
}
