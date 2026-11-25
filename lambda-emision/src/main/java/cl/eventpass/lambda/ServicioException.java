package cl.eventpass.lambda;

/** Falla transitoria al hablar con un microservicio: el mensaje vuelve a la cola y se reintenta. */
public class ServicioException extends RuntimeException {
    public ServicioException(String mensaje) {
        super(mensaje);
    }

    public ServicioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
