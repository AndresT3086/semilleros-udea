package co.udea.semilleros.domain.port.in;

import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.reporte.FormatoExportacion;
import co.udea.semilleros.domain.model.reporte.OrdenRendimiento;
import co.udea.semilleros.domain.model.reporte.ReporteArchivo;
import co.udea.semilleros.domain.model.reporte.ReporteDashboard;
import co.udea.semilleros.domain.model.reporte.ReporteFiltro;
import co.udea.semilleros.domain.model.reporte.ReporteKpis;
import co.udea.semilleros.domain.model.reporte.ReporteOpcion;
import co.udea.semilleros.domain.model.reporte.ReporteRendimiento;

import java.util.List;

public interface ConsultarReportesUseCase {

    ReporteKpis obtenerKpis(ReporteFiltro filtro);

    ReporteDashboard obtenerDashboard(ReporteFiltro filtro);

    PageResult<ReporteRendimiento> obtenerRendimiento(ReporteFiltro filtro, int pagina, int tamano,
                                                      OrdenRendimiento orden, boolean ascendente);

    List<ReporteOpcion> listarSemilleros(ReporteFiltro filtro);

    ReporteArchivo exportar(ReporteFiltro filtro, FormatoExportacion formato,
                            OrdenRendimiento orden, boolean ascendente);

    String huellaDatos();
}
