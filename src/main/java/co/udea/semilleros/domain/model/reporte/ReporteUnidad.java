package co.udea.semilleros.domain.model.reporte;

/**
 * Semilleros e integrantes (personas únicas por cédula) de una unidad académica (HU3, HU6).
 */
public record ReporteUnidad(Long id, String nombre, TipoUnidad tipo, long semilleros, long estudiantes) {
}
