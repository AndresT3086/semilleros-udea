package co.udea.semilleros.domain.model.reporte;

import java.util.List;

/**
 * Todos los agregados del tablero de reportes para un mismo conjunto de filtros.
 */
public record ReporteDashboard(
        ReporteKpis kpis,
        List<ReporteUnidad> porUnidad,
        List<ReporteConteo> porCampus,
        List<ReporteUnidad> topFacultades,
        List<ReporteConteo> porSexo,
        List<ReporteConteo> porRol,
        List<ReporteEvolucion> evolucion,
        List<ReporteConteo> actividadesPorTipo
) {
}
