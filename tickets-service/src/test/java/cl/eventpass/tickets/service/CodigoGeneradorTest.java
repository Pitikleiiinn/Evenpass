package cl.eventpass.tickets.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodigoGeneradorTest {

    private final CodigoGenerador generador = new CodigoGenerador();

    @Test
    void generar_respetaElFormatoSinCaracteresAmbiguos() {
        for (int i = 0; i < 200; i++) {
            String codigo = generador.generar();
            assertTrue(codigo.matches("^EP-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$"), codigo);
        }
    }

    @Test
    void generar_noRepiteCodigosEnUnaMuestraGrande() {
        Set<String> codigos = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            codigos.add(generador.generar());
        }
        assertEquals(5000, codigos.size());
    }
}
