package co.udea.semilleros.domain.model.reporte;

/**
 * Fila de la tabla de rendimiento por semillero (HU9).
 *
 * @param actividadesRealizadas tipos de actividad que el semillero declara realizar (caracterización)
 * @param sesiones              actividades registradas con asistencia en el período
 * @param porcentajeAsistencia  presentes / (presentes + ausentes) en esas actividades; null si no hay registros
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
        long sesiones,
        Double porcentajeAsistencia,
        String estado
) {
}
