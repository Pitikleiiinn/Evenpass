package cl.eventpass.tickets.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protege el microservicio para que NO sea accesible directamente desde internet:
 * <ul>
 *   <li>Rutas /internal/**: solo las puede llamar otro componente del sistema (la funcion Lambda)
 *       enviando la cabecera X-Internal-Key.</li>
 *   <li>Resto de rutas: si GATEWAY_KEY esta configurada, solo se aceptan peticiones que pasen por
 *       Amazon API Gateway, que inyecta la cabecera X-Gateway-Key.</li>
 * </ul>
 * En desarrollo local GATEWAY_KEY va vacia y se puede probar directo con el navegador/Postman.
 */
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_GATEWAY = "X-Gateway-Key";
    public static final String HEADER_INTERNAL = "X-Internal-Key";

    private final String gatewayKey;
    private final String internalKey;

    public ApiKeyFilter(String gatewayKey, String internalKey) {
        this.gatewayKey = gatewayKey;
        this.internalKey = internalKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.startsWith("/actuator/health")) {
            chain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/internal/")) {
            if (!coincide(internalKey, request.getHeader(HEADER_INTERNAL))) {
                rechazar(response, "Clave interna invalida");
                return;
            }
        } else if (gatewayKey != null && !gatewayKey.isBlank()
                && !coincide(gatewayKey, request.getHeader(HEADER_GATEWAY))) {
            rechazar(response, "Acceso permitido solo a traves del API Gateway");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean coincide(String esperado, String recibido) {
        if (esperado == null || esperado.isBlank() || recibido == null) {
            return false;
        }
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8), recibido.getBytes(StandardCharsets.UTF_8));
    }

    private void rechazar(HttpServletResponse response, String mensaje) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + mensaje + "\"}");
    }
}
