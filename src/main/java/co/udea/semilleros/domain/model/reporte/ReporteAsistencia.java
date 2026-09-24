package co.udea.semilleros.domain.model.reporte;

import co.udea.semilleros.domain.model.asistencia.ConteoAsistencia;

/**
 * Asistencia agregada de los semilleros filtrados: se suman todas las asistencias
 * esperadas (no se promedian porcentajes de semilleros de distinto tamaño).
 */
public record ReporteAsistencia(long sesiones, ConteoAsistencia asistencia) {
}
