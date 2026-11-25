package cl.eventpass.ordenes.service;

import cl.eventpass.ordenes.dto.OrdenRequest;
import cl.eventpass.ordenes.dto.OrdenResponse;
import cl.eventpass.ordenes.messaging.OrdenMensaje;
import cl.eventpass.ordenes.messaging.OrdenPublisher;
import cl.eventpass.ordenes.model.EstadoOrden;
import cl.eventpass.ordenes.model.Orden;
import cl.eventpass.ordenes.repository.OrdenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenServiceTest {

    @Mock
    private OrdenRepository ordenes;
    @Mock
    private OrdenPublisher publisher;

    private OrdenService servicio;

    @BeforeEach
    void setUp() {
        servicio = new OrdenService(ordenes, publisher);
    }

    @Test
    void crear_aceptaLaOrdenComoPendienteYLaEncola() {
        when(ordenes.save(any(Orden.class))).thenAnswer(inv -> inv.getArgument(0));

        OrdenResponse resp = servicio.crear("7", new OrdenRequest(1L, 2));

        assertEquals("PENDIENTE_EMISION", resp.estado());
        ArgumentCaptor<OrdenMensaje> captor = ArgumentCaptor.forClass(OrdenMensaje.class);
        verify(publisher).publicar(captor.capture());
        assertEquals(resp.id(), captor.getValue().ordenId());
        assertEquals(1L, captor.getValue().eventoId());
        assertEquals("7", captor.getValue().compradorId());
        assertEquals(2, captor.getValue().cantidad());
    }

    @Test
    void crear_siFallaSqs_marcaRechazadaYResponde503() {
        when(ordenes.save(any(Orden.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SQS caido")).when(publisher).publicar(any(OrdenMensaje.class));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.crear("7", new OrdenRequest(1L, 1)));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        ArgumentCaptor<Orden> captor = ArgumentCaptor.forClass(Orden.class);
        verify(ordenes, times(2)).save(captor.capture());
        assertEquals(EstadoOrden.RECHAZADA, captor.getValue().getEstado());
    }

    @Test
    void obtenerDe_ordenDeOtroComprador_da404() {
        Orden ajena = new Orden("o-1", 1L, "99", 1);
        when(ordenes.findById("o-1")).thenReturn(Optional.of(ajena));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.obtenerDe("o-1", "7"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void actualizarEstado_pendienteAEmitida_cambiaElEstado() {
        Orden orden = new Orden("o-2", 1L, "7", 1);
        when(ordenes.findById("o-2")).thenReturn(Optional.of(orden));
        when(ordenes.save(any(Orden.class))).thenAnswer(inv -> inv.getArgument(0));

        OrdenResponse resp = servicio.actualizarEstado("o-2", EstadoOrden.EMITIDA, null);

        assertEquals("EMITIDA", resp.estado());
    }

    @Test
    void actualizarEstado_repetirElMismoEstado_esIdempotente() {
        Orden orden = new Orden("o-3", 1L, "7", 1);
        orden.cambiarEstado(EstadoOrden.EMITIDA, null);
        when(ordenes.findById("o-3")).thenReturn(Optional.of(orden));

        OrdenResponse resp = servicio.actualizarEstado("o-3", EstadoOrden.EMITIDA, null);

        assertEquals("EMITIDA", resp.estado());
        verify(ordenes, never()).save(any(Orden.class));
    }

    @Test
    void actualizarEstado_desdeEstadoFinalADistinto_lanza409() {
        Orden orden = new Orden("o-4", 1L, "7", 1);
        orden.cambiarEstado(EstadoOrden.EMITIDA, null);
        when(ordenes.findById("o-4")).thenReturn(Optional.of(orden));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.actualizarEstado("o-4", EstadoOrden.RECHAZADA, "sin cupo"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
