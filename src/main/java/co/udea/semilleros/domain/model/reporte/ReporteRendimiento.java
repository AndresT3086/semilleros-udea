package co.udea.semilleros.domain.model.reporte;

/**
 * Fila de la tabla de rendimiento por semillero (HU9).
 * {@code porcentajeAsistencia} es null mientras el sistema no registre asistencia.
 */
public record ReporteRendimiento(
        Long id,
        String nombre,
        String codigo,
        String unidadAcademica,
        TipoUnidad tipoUnidad,
        String campus,
        long participantes,
        long actividadesRealizadas,
        Double porcentajeAsistencia,
        String estado
) {
}
