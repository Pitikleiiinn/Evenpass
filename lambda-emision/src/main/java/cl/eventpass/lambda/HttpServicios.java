package cl.eventpass.lambda;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Implementacion de {@link Servicios} sobre HTTP. Llama a los endpoints /internal/** de los
 * microservicios con la cabecera X-Internal-Key.
 *
 * Variables de entorno: EVENTOS_URL, ORDENES_URL, TICKETS_URL, INTERNAL_API_KEY.
 */
public class HttpServicios implements Servicios {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String eventosUrl;
    private final String ordenesUrl;
    private final String ticketsUrl;
    private final String internalKey;

    public HttpServicios(String eventosUrl, String ordenesUrl, String ticketsUrl, String internalKey) {
        this.eventosUrl = sinBarraFinal(eventosUrl);
        this.ordenesUrl = sinBarraFinal(ordenesUrl);
        this.ticketsUrl = sinBarraFinal(ticketsUrl);
        this.internalKey = internalKey;
    }

    public static HttpServicios desdeEntorno() {
        return new HttpServicios(
                requerida("EVENTOS_URL"), requerida("ORDENES_URL"), requerida("TICKETS_URL"),
                requerida("INTERNAL_API_KEY"));
    }

    @Override
    public String obtenerEstadoOrden(String ordenId) {
        HttpResponse<String> r = enviar("GET", ordenesUrl + "/internal/ordenes/" + ordenId, null);
        if (r.statusCode() != 200) {
            // 404: la orden aun no es visible o no existe -> se reintenta
            throw new ServicioException("ordenes-service respondio " + r.statusCode() + " al consultar " + ordenId);
        }
        try {
            return mapper.readTree(r.body()).path("estado").asText();
        } catch (IOException e) {
            throw new ServicioException("Respuesta invalida de ordenes-service", e);
        }
    }

    @Override
    public ResultadoReserva reservarAforo(OrdenMensaje orden) {
        HttpResponse<String> r = enviar("POST", eventosUrl + "/internal/reservas", Map.of(
                "ordenId", orden.ordenId(), "eventoId", orden.eventoId(), "cantidad", orden.cantidad()));
        return switch (r.statusCode()) {
            case 200 -> ResultadoReserva.RESERVADO;
            case 409 -> ResultadoReserva.SIN_CUPO;
            case 404 -> ResultadoReserva.EVENTO_NO_EXISTE;
            default -> throw new ServicioException("eventos-service respondio " + r.statusCode() + " al reservar");
        };
    }

    @Override
    public void liberarAforo(String ordenId) {
        HttpResponse<String> r = enviar("DELETE", eventosUrl + "/internal/reservas/" + ordenId, null);
        if (r.statusCode() != 204 && r.statusCode() != 404) {
            throw new ServicioException("eventos-service respondio " + r.statusCode() + " al liberar aforo");
        }
    }

    @Override
    public void emitirTickets(OrdenMensaje orden) {
        HttpResponse<String> r = enviar("POST", ticketsUrl + "/internal/tickets", Map.of(
                "ordenId", orden.ordenId(), "eventoId", orden.eventoId(),
                "compradorId", orden.compradorId(), "cantidad", orden.cantidad()));
        if (r.statusCode() != 201 && r.statusCode() != 200) {
            throw new ServicioException("tickets-service respondio " + r.statusCode() + " al emitir tickets");
        }
    }

    @Override
    public void actualizarOrden(String ordenId, String estado, String motivo) {
        Map<String, Object> cuerpo = motivo == null
                ? Map.of("estado", estado)
                : Map.of("estado", estado, "motivo", motivo);
        HttpResponse<String> r = enviar("PATCH", ordenesUrl + "/internal/ordenes/" + ordenId + "/estado", cuerpo);
        if (r.statusCode() != 200) {
            throw new ServicioException("ordenes-service respondio " + r.statusCode() + " al actualizar la orden");
        }
    }

    private HttpResponse<String> enviar(String metodo, String url, Object cuerpo) {
        try {
            HttpRequest.BodyPublisher body = cuerpo == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(cuerpo));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Key", internalKey)
                    .method(metodo, body)
                    .build();
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ServicioException("No se pudo conectar con " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicioException("Llamada interrumpida a " + url, e);
        }
    }

    private static String requerida(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + nombre);
        }
        return valor;
    }

    private static String sinBarraFinal(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
