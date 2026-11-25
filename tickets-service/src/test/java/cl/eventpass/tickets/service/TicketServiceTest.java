package cl.eventpass.tickets.service;

import cl.eventpass.tickets.dto.EmitirRequest;
import cl.eventpass.tickets.dto.TicketResponse;
import cl.eventpass.tickets.dto.ValidacionResponse;
import cl.eventpass.tickets.model.Ticket;
import cl.eventpass.tickets.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository tickets;

    private TicketService servicio;

    @BeforeEach
    void setUp() {
        servicio = new TicketService(tickets, new CodigoGenerador());
    }

    @Test
    void emitir_ordenNueva_creaUnTicketPorEntradaConCodigosDistintos() {
        when(tickets.findByOrdenIdOrderByNumero("o-1")).thenReturn(List.of());
        when(tickets.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<TicketResponse> resp = servicio.emitir(new EmitirRequest("o-1", 5L, "7", 3));

        assertEquals(3, resp.size());
        assertEquals(3, resp.stream().map(TicketResponse::codigo).distinct().count());
        assertTrue(resp.stream().allMatch(t -> "EMITIDO".equals(t.estado())));
    }

    @Test
    void emitir_ordenYaEmitida_esIdempotenteYNoCreaTicketsNuevos() {
        Ticket existente = new Ticket("EP-AAAA-BBBB-CCCC", "o-2", 1, 5L, "7");
        when(tickets.findByOrdenIdOrderByNumero("o-2")).thenReturn(List.of(existente));

        List<TicketResponse> resp = servicio.emitir(new EmitirRequest("o-2", 5L, "7", 1));

        assertEquals("EP-AAAA-BBBB-CCCC", resp.get(0).codigo());
        verify(tickets, never()).saveAll(anyList());
    }

    @Test
    void validar_codigoEmitido_autorizaIngreso() {
        Ticket t = new Ticket("EP-AAAA-BBBB-CCCC", "o-3", 1, 5L, "7");
        when(tickets.marcarUsado(any(String.class), any(Instant.class))).thenReturn(1);
        when(tickets.findByCodigo("EP-AAAA-BBBB-CCCC")).thenReturn(Optional.of(t));

        ValidacionResponse resp = servicio.validar("  ep-aaaa-bbbb-cccc ");

        assertTrue(resp.valido());
        assertEquals(5L, resp.eventoId());
    }

    @Test
    void validar_codigoYaUsado_lanza409() {
        Ticket t = new Ticket("EP-AAAA-BBBB-CCCC", "o-4", 1, 5L, "7");
        when(tickets.marcarUsado(any(String.class), any(Instant.class))).thenReturn(0);
        when(tickets.findByCodigo("EP-AAAA-BBBB-CCCC")).thenReturn(Optional.of(t));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.validar("EP-AAAA-BBBB-CCCC"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void validar_codigoInexistente_lanza404() {
        when(tickets.marcarUsado(any(String.class), any(Instant.class))).thenReturn(0);
        when(tickets.findByCodigo("EP-ZZZZ-ZZZZ-ZZZZ")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.validar("EP-ZZZZ-ZZZZ-ZZZZ"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
