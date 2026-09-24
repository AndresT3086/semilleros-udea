package co.udea.semilleros.domain.model.reporte;

import co.udea.semilleros.domain.exception.FiltroReporteInvalidoException;

import java.util.Locale;

/**
 * Columnas por las que se puede ordenar la tabla de rendimiento (HU9).
 */
public enum OrdenRendimiento {
    NOMBRE, UNIDAD, TIPO, CAMPUS, PARTICIPANTES, ACTIVIDADES, SESIONES, ASISTENCIA, ESTADO;

    public static OrdenRendimiento de(String valor) {
        if (valor == null || valor.isBlank()) {
            return NOMBRE;
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new FiltroReporteInvalidoException("No se puede ordenar por: " + valor + ".");
        }
    }
}
