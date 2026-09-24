package co.udea.semilleros.domain.model.reporte;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos que se exportan: el tablero y la tabla completa con los filtros aplicados (RN35).
 */
public record ReporteCompleto(ReporteFiltro filtro, ReporteDashboard dashboard,
                              List<ReporteRendimiento> rendimiento, LocalDateTime generadoEn) {
}
