package cl.eventpass.ordenes.messaging;

/** Mensaje que viaja por la cola SQS hacia la funcion Lambda de emision. */
public record OrdenMensaje(String ordenId, Long eventoId, String compradorId, int cantidad) {
}
