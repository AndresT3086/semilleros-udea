package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.AccesoNoAutorizadoException;
import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.reporte.AlcanceReporte;
import co.udea.semilleros.domain.model.reporte.FormatoExportacion;
import co.udea.semilleros.domain.model.reporte.OrdenRendimiento;
import co.udea.semilleros.domain.model.reporte.ReporteArchivo;
import co.udea.semilleros.domain.model.reporte.ReporteCompleto;
import co.udea.semilleros.domain.model.reporte.ReporteDashboard;
import co.udea.semilleros.domain.model.reporte.ReporteEvolucion;
import co.udea.semilleros.domain.model.reporte.ReporteFiltro;
import co.udea.semilleros.domain.model.reporte.ReporteKpis;
import co.udea.semilleros.domain.model.reporte.ReporteOpcion;
import co.udea.semilleros.domain.model.reporte.ReporteRendimiento;
import co.udea.semilleros.domain.model.reporte.ReporteUnidad;
import co.udea.semilleros.domain.model.reporte.TipoUnidad;
import co.udea.semilleros.domain.port.in.ConsultarReportesUseCase;
import co.udea.semilleros.domain.port.out.ExportadorReportePort;
import co.udea.semilleros.domain.port.out.ReportesRepositoryPort;
import co.udea.semilleros.domain.port.out.SemilleroRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultarReportesUseCaseImpl implements ConsultarReportesUseCase {

    static final int TOP_FACULTADES = 5;
    static final int TAMANO_MAXIMO_PAGINA = 100;
    static final int MINIMO_ANIOS_PARA_PROYECCION = 3;
    private static final DateTimeFormatter FORMATO_NOMBRE_ARCHIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

    private final ReportesRepositoryPort reportesRepositoryPort;
    private final SemilleroRepositoryPort semilleroRepositoryPort;
    private final ExportadorReportePort exportadorReportePort;
    private final Clock clock;

    @Override
    public ReporteKpis obtenerKpis(ReporteFiltro filtro) {
        validarAlcance(filtro);
        LocalDate hoy = LocalDate.now(clock);
        ReporteFiltro anterior = filtro.periodoAnterior(hoy);

        long semilleros = reportesRepositoryPort.contarSemillerosActivos(filtro);
        long registrados = reportesRepositoryPort.contarIntegrantesRegistrados(filtro);
        long activos = reportesRepositoryPort.contarIntegrantesActivos(filtro);
        long actividades = reportesRepositoryPort.contarActividadesRealizadas(filtro);
        Double tasa = porcentaje(activos, registrados);

        long semillerosAntes = reportesRepositoryPort.contarSemillerosActivos(anterior);
        long registradosAntes = reportesRepositoryPort.contarIntegrantesRegistrados(anterior);
        long activosAntes = reportesRepositoryPort.contarIntegrantesActivos(anterior);
        long actividadesAntes = reportesRepositoryPort.contarActividadesRealizadas(anterior);
        Double tasaAntes = porcentaje(activosAntes, registradosAntes);

        ReporteKpis.Tendencias tendencias = new ReporteKpis.Tendencias(
                variacion(semilleros, semillerosAntes),
                variacion(registrados, registradosAntes),
                variacion(activos, activosAntes),
                variacion(actividades, actividadesAntes),
                tasa == null || tasaAntes == null ? null : redondear(tasa - tasaAntes));

        String comparado = anterior.periodo() != null ? anterior.periodo() : "mes anterior";
        return new ReporteKpis(semilleros, registrados, activos, actividades, tasa, tendencias,
                comparado, LocalDateTime.now(clock), filtro.alcance());
    }

    @Override
    public ReporteDashboard obtenerDashboard(ReporteFiltro filtro) {
        ReporteKpis kpis = obtenerKpis(filtro);
        List<ReporteUnidad> porUnidad = reportesRepositoryPort.distribucionPorUnidad(filtro);
        return new ReporteDashboard(
                kpis,
                porUnidad,
                reportesRepositoryPort.distribucionPorCampus(filtro),
                topFacultades(porUnidad),
                reportesRepositoryPort.integrantesPorSexo(filtro),
                reportesRepositoryPort.integrantesPorRol(filtro),
                evolucion(filtro),
                reportesRepositoryPort.actividadesPorTipo(filtro),
                reportesRepositoryPort.asistencia(filtro));
    }

    @Override
    public PageResult<ReporteRendimiento> obtenerRendimiento(ReporteFiltro filtro, int pagina, int tamano,
                                                             OrdenRendimiento orden, boolean ascendente) {
        validarAlcance(filtro);
        validarDetallePermitido(filtro);
        int paginaValida = Math.max(0, pagina);
        int tamanoValido = Math.clamp(tamano, 1, TAMANO_MAXIMO_PAGINA);
        return reportesRepositoryPort.rendimiento(filtro, paginaValida, tamanoValido, orden, ascendente);
    }

    @Override
    public List<ReporteOpcion> listarSemilleros(ReporteFiltro filtro) {
        validarDetallePermitido(filtro);
        return reportesRepositoryPort.semillerosActivos(filtro.sinSemillero());
    }

    @Override
    public ReporteArchivo exportar(ReporteFiltro filtro, FormatoExportacion formato,
                                   OrdenRendimiento orden, boolean ascendente) {
        validarDetallePermitido(filtro);
        ReporteDashboard dashboard = obtenerDashboard(filtro);
        List<ReporteRendimiento> filas = reportesRepositoryPort.rendimientoCompleto(filtro, orden, ascendente);
        LocalDateTime ahora = LocalDateTime.now(clock);
        byte[] contenido = exportadorReportePort.exportar(
                new ReporteCompleto(filtro, dashboard, filas, ahora), formato);
        String nombre = "reporte_sigsi_" + ahora.format(FORMATO_NOMBRE_ARCHIVO) + "." + formato.getExtension();
        return new ReporteArchivo(nombre, formato.getContentType(), contenido);
    }

    @Override
    public String huellaDatos() {
        return reportesRepositoryPort.huellaDatos();
    }

    /**
     * RN43/RN45: un coordinador solo consulta sus semilleros y el público nunca
     * accede al detalle de un semillero específico.
     */
    private void validarAlcance(ReporteFiltro filtro) {
        if (filtro.idSemillero() == null) {
            return;
        }
        if (filtro.alcance() == AlcanceReporte.PUBLICO) {
            throw new AccesoNoAutorizadoException("reporte de un semillero específico");
        }
        if (filtro.alcance() == AlcanceReporte.COORDINADOR) {
            boolean esPropio = semilleroRepositoryPort.buscarPorId(filtro.idSemillero())
                    .map(semillero -> Objects.equals(semillero.getIdCoordinador(), filtro.idCoordinador()))
                    .orElse(false);
            if (!esPropio) {
                throw new AccesoNoAutorizadoException("reporte del semillero " + filtro.idSemillero());
            }
        }
    }

    /** RN44: el alcance público solo ve información agregada y anonimizada. */
    private void validarDetallePermitido(ReporteFiltro filtro) {
        if (filtro.alcance() == AlcanceReporte.PUBLICO) {
            throw new AccesoNoAutorizadoException("detalle de semilleros");
        }
    }

    /** RN21: se incluyen todas las facultades empatadas con la quinta posición. */
    List<ReporteUnidad> topFacultades(List<ReporteUnidad> porUnidad) {
        List<ReporteUnidad> facultades = porUnidad.stream()
                .filter(unidad -> unidad.tipo() == TipoUnidad.FACULTAD && unidad.semilleros() > 0)
                .sorted(Comparator.comparingLong(ReporteUnidad::semilleros).reversed()
                        .thenComparing(ReporteUnidad::nombre))
                .toList();
        if (facultades.size() <= TOP_FACULTADES) {
            return facultades;
        }
        long corte = facultades.get(TOP_FACULTADES - 1).semilleros();
        return facultades.stream().filter(unidad -> unidad.semilleros() >= corte).toList();
    }

    /**
     * RN24: desde el año del primer semillero hasta el año de corte (o el actual),
     * con el acumulado de semilleros activos. RN27: proyección lineal del año
     * siguiente cuando hay al menos tres años de datos.
     */
    List<ReporteEvolucion> evolucion(ReporteFiltro filtro) {
        Map<Integer, Long> creados = reportesRepositoryPort.semillerosCreadosPorAnio(filtro.sinPeriodo());
        if (creados.isEmpty()) {
            return List.of();
        }
        int desde = creados.keySet().stream().min(Integer::compare).orElseThrow();
        int hasta = filtro.anioCorte() != null ? filtro.anioCorte() : LocalDate.now(clock).getYear();
        List<ReporteEvolucion> puntos = new ArrayList<>();
        long acumulado = 0;
        for (int anio = desde; anio <= hasta; anio++) {
            long nuevos = creados.getOrDefault(anio, 0L);
            acumulado += nuevos;
            puntos.add(new ReporteEvolucion(anio, acumulado, nuevos, false));
        }
        if (puntos.size() >= MINIMO_ANIOS_PARA_PROYECCION) {
            puntos.add(proyectar(puntos));
        }
        return puntos;
    }

    private ReporteEvolucion proyectar(List<ReporteEvolucion> puntos) {
        int n = puntos.size();
        double mediaX = puntos.stream().mapToInt(ReporteEvolucion::anio).average().orElse(0);
        double mediaY = puntos.stream().mapToLong(ReporteEvolucion::semillerosActivos).average().orElse(0);
        double covarianza = 0;
        double varianza = 0;
        for (ReporteEvolucion punto : puntos) {
            covarianza += (punto.anio() - mediaX) * (punto.semillerosActivos() - mediaY);
            varianza += Math.pow(punto.anio() - mediaX, 2);
        }
        double pendiente = varianza == 0 ? 0 : covarianza / varianza;
        int siguiente = puntos.get(n - 1).anio() + 1;
        long ultimo = puntos.get(n - 1).semillerosActivos();
        long estimado = Math.round(mediaY + pendiente * (siguiente - mediaX));
        // Los activos acumulados no decrecen: la proyección nunca queda por debajo del último valor
        long proyectado = Math.max(ultimo, estimado);
        return new ReporteEvolucion(siguiente, proyectado, proyectado - ultimo, true);
    }

    static Double porcentaje(long parte, long total) {
        return total == 0 ? null : redondear(parte * 100.0 / total);
    }

    static Double variacion(long actual, long anterior) {
        return anterior == 0 ? null : redondear((actual - anterior) * 100.0 / anterior);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
