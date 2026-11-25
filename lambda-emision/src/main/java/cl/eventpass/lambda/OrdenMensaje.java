package cl.eventpass.lambda;

/** Mensaje publicado por ordenes-service en la cola SQS. */
public record OrdenMensaje(String ordenId, Long eventoId, String compradorId, int cantidad) {
}
