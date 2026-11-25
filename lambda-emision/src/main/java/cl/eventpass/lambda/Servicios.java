package cl.eventpass.lambda;

/**
 * Operaciones que la funcion necesita de los microservicios. Es una interfaz para poder probar
 * la logica de la funcion sin red (ver EmisionTicketsHandlerTest).
 */
public interface Servicios {

    /** Estado actual de la orden en ordenes-service (PENDIENTE_EMISION, EMITIDA, RECHAZADA). */
    String obtenerEstadoOrden(String ordenId);

    /** eventos-service: verifica y descuenta el aforo (idempotente por ordenId). */
    ResultadoReserva reservarAforo(OrdenMensaje orden);

    /** eventos-service: devuelve los cupos reservados (compensacion). */
    void liberarAforo(String ordenId);

    /** tickets-service: genera los codigos de acceso (idempotente por ordenId). */
    void emitirTickets(OrdenMensaje orden);

    /** ordenes-service: deja la orden en EMITIDA o RECHAZADA. */
    void actualizarOrden(String ordenId, String estado, String motivo);
}
