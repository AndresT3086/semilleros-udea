package co.udea.semilleros.domain.model.reporte;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

/**
 * Tipo de unidad académica. Se deriva de la primera palabra del nombre oficial
 * ("Facultad de Artes", "Escuela de Idiomas", "Instituto de Filosofía", ...).
 */
public enum TipoUnidad {
    FACULTAD("facultad"),
    ESCUELA("escuela"),
    INSTITUTO("instituto"),
    CORPORACION("corporacion"),
    SECCIONAL("seccional"),
    OTRA(null);

    private final String prefijo;

    TipoUnidad(String prefijo) {
        this.prefijo = prefijo;
    }

    /** Prefijo en minúsculas y sin tildes con el que empieza el nombre de la unidad. */
    public String getPrefijo() {
        return prefijo;
    }

    public static TipoUnidad desdeNombre(String nombreUnidad) {
        if (nombreUnidad == null) {
            return OTRA;
        }
        String normalizado = sinTildes(nombreUnidad.trim()).toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(tipo -> tipo.prefijo != null && normalizado.startsWith(tipo.prefijo))
                .findFirst()
                .orElse(OTRA);
    }

    static String sinTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
