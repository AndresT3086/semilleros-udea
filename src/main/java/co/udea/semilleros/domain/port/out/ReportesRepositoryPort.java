package co.udea.semilleros.domain.port.out;

import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.reporte.OrdenRendimiento;
import co.udea.semilleros.domain.model.reporte.ReporteAsistencia;
import co.udea.semilleros.domain.model.reporte.ReporteConteo;
import co.udea.semilleros.domain.model.reporte.ReporteFiltro;
import co.udea.semilleros.domain.model.reporte.ReporteOpcion;
import co.udea.semilleros.domain.model.reporte.ReporteRendimiento;
import co.udea.semilleros.domain.model.reporte.ReporteUnidad;

import java.util.List;
import java.util.Map;

/**
 * Consultas agregadas de solo lectura para los reportes. Todas respetan los filtros
 * y el alcance de {@link ReporteFiltro}.
 */
public interface ReportesRepositoryPort {

    long contarSemillerosActivos(ReporteFiltro filtro);

    /** Personas únicas (por cédula) vinculadas hasta la fecha de corte. */
    long contarIntegrantesRegistrados(ReporteFiltro filtro);

    /** Personas únicas (por cédula) con vinculación activa hasta la fecha de corte. */
    long contarIntegrantesActivos(ReporteFiltro filtro);

    long contarActividadesRealizadas(ReporteFiltro filtro);

    List<ReporteUnidad> distribucionPorUnidad(ReporteFiltro filtro);

    List<ReporteConteo> distribucionPorCampus(ReporteFiltro filtro);

    List<ReporteConteo> integrantesPorSexo(ReporteFiltro filtro);

    List<ReporteConteo> integrantesPorRol(ReporteFiltro filtro);

    /** Semilleros activos creados cada año (solo los que tienen año de creación). */
    Map<Integer, Long> semillerosCreadosPorAnio(ReporteFiltro filtro);

    List<ReporteConteo> actividadesPorTipo(ReporteFiltro filtro);

    /** Actividades registradas en el período y asistencia agregada de los semilleros filtrados. */
    ReporteAsistencia asistencia(ReporteFiltro filtro);

    PageResult<ReporteRendimiento> rendimiento(ReporteFiltro filtro, int pagina, int tamano,
                                               OrdenRendimiento orden, boolean ascendente);

    List<ReporteRendimiento> rendimientoCompleto(ReporteFiltro filtro, OrdenRendimiento orden, boolean ascendente);

    List<ReporteOpcion> semillerosActivos(ReporteFiltro filtro);

    /** Resumen que cambia cuando cambian los datos que alimentan los reportes (HU13). */
    String huellaDatos();
}
