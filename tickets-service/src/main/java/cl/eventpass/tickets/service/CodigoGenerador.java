package cl.eventpass.tickets.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Genera codigos de acceso no adivinables con formato EP-XXXX-XXXX-XXXX.
 * El alfabeto omite caracteres ambiguos (0/O, 1/I) para facilitar la lectura en puerta.
 */
@Component
public class CodigoGenerador {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom azar = new SecureRandom();

    public String generar() {
        StringBuilder sb = new StringBuilder("EP");
        for (int grupo = 0; grupo < 3; grupo++) {
            sb.append('-');
            for (int i = 0; i < 4; i++) {
                sb.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
            }
        }
        return sb.toString();
    }
}
