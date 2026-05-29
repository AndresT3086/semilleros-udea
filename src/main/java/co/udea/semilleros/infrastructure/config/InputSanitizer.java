package co.udea.semilleros.infrastructure.config;

import org.springframework.stereotype.Component;

@Component
public class InputSanitizer {

    private static final int MAX_LENGTH_TEXTO_LIBRE = 5000;

    /**
     * Elimina caracteres de control y etiquetas HTML básicas.
     * No usa librerías externas para mantener la dependencia mínima.
     */
    public String sanitizar(String input) {
        if (input == null) return null;

        String sanitizado = input
                // Eliminar etiquetas HTML
                .replaceAll("<[^>]*>", "")
                // Eliminar caracteres de control excepto saltos de línea y tabulaciones
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                .trim();

        // Truncar si excede el máximo
        if (sanitizado.length() > MAX_LENGTH_TEXTO_LIBRE) {
            sanitizado = sanitizado.substring(0, MAX_LENGTH_TEXTO_LIBRE);
        }

        return sanitizado;
    }

    public String sanitizarCampoCorto(String input) {
        if (input == null) return null;
        return input
                .replaceAll("<[^>]*>", "")
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                .trim();
    }
}
