package cl.eventpass.ordenes.messaging;

public interface OrdenPublisher {
    void publicar(OrdenMensaje mensaje);
}
