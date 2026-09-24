package co.udea.semilleros.domain.model.reporte;

/**
 * Punto de la evolución anual (HU7): semilleros activos acumulados al cierre del año,
 * semilleros creados ese año y si el valor es una proyección (RN27).
 */
public record ReporteEvolucion(int anio, long semillerosActivos, long nuevos, boolean proyectado) {
}
