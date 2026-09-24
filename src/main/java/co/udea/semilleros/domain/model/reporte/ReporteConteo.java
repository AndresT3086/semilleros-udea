package co.udea.semilleros.domain.model.reporte;

/**
 * Categoría con su cantidad absoluta (sexo, rol, tipo de actividad, campus...).
 */
public record ReporteConteo(String id, String nombre, long cantidad) {
}
