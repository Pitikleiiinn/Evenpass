package cl.eventpass.ordenes.model;

public enum EstadoOrden {
    /** La orden fue aceptada y espera que la funcion Lambda verifique aforo y emita el ticket. */
    PENDIENTE_EMISION,
    /** Aforo verificado y ticket(s) generado(s). */
    EMITIDA,
    /** No hubo cupos (o el evento no existe): no se convierte en ticket. */
    RECHAZADA
}
