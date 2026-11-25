package cl.eventpass.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiKeyFilterTest {

    private MockHttpServletResponse ejecutar(ApiKeyFilter filtro, MockHttpServletRequest req, MockFilterChain chain)
            throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filtro.doFilter(req, resp, chain);
        return resp;
    }

    @Test
    void rutaInterna_sinClave_responde403() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse resp = ejecutar(new ApiKeyFilter("", "interna"),
                new MockHttpServletRequest("POST", "/internal/reservas"), chain);

        assertEquals(403, resp.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void rutaInterna_conClaveCorrecta_pasa() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/internal/reservas");
        req.addHeader(ApiKeyFilter.HEADER_INTERNAL, "interna");
        MockFilterChain chain = new MockFilterChain();

        ejecutar(new ApiKeyFilter("", "interna"), req, chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    void conGatewayKeyConfigurada_rechazaPeticionesDirectas() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse resp = ejecutar(new ApiKeyFilter("gw-secreta", "interna"),
                new MockHttpServletRequest("GET", "/eventos"), chain);

        assertEquals(403, resp.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void conGatewayKeyConfigurada_aceptaPeticionesDelGateway() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/eventos");
        req.addHeader(ApiKeyFilter.HEADER_GATEWAY, "gw-secreta");
        MockFilterChain chain = new MockFilterChain();

        ejecutar(new ApiKeyFilter("gw-secreta", "interna"), req, chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    void sinGatewayKeyConfigurada_modoDesarrolloPermiteAccesoDirecto() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        ejecutar(new ApiKeyFilter("", "interna"), new MockHttpServletRequest("GET", "/auth/me"), chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    void healthCheck_siempreAccesible() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        ejecutar(new ApiKeyFilter("gw-secreta", "interna"),
                new MockHttpServletRequest("GET", "/actuator/health"), chain);

        assertNotNull(chain.getRequest());
    }
}
