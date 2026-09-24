package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.AccesoNoAutorizadoException;
import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.model.asistencia.ConteoAsistencia;
import co.udea.semilleros.domain.model.reporte.AlcanceReporte;
import co.udea.semilleros.domain.model.reporte.FormatoExportacion;
import co.udea.semilleros.domain.model.reporte.OrdenRendimiento;
import co.udea.semilleros.domain.model.reporte.ReporteArchivo;
import co.udea.semilleros.domain.model.reporte.ReporteAsistencia;
import co.udea.semilleros.domain.model.reporte.ReporteCompleto;
import co.udea.semilleros.domain.model.reporte.ReporteConteo;
import co.udea.semilleros.domain.model.reporte.ReporteDashboard;
import co.udea.semilleros.domain.model.reporte.ReporteEvolucion;
import co.udea.semilleros.domain.model.reporte.ReporteFiltro;
import co.udea.semilleros.domain.model.reporte.ReporteKpis;
import co.udea.semilleros.domain.model.reporte.ReporteOpcion;
import co.udea.semilleros.domain.model.reporte.ReporteRendimiento;
import co.udea.semilleros.domain.model.reporte.ReporteUnidad;
import co.udea.semilleros.domain.model.reporte.TipoUnidad;
import co.udea.semilleros.domain.port.out.ExportadorReportePort;
import co.udea.semilleros.domain.port.out.ReportesRepositoryPort;
import co.udea.semilleros.domain.port.out.SemilleroRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarReportesUseCaseImpl - Pruebas unitarias")
class ConsultarReportesUseCaseImplTest {

    // 24 de septiembre de 2026, 10:30 en Bogotá
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-24T15:30:00Z"), ZoneId.of("America/Bogota"));

    @Mock
    private ReportesRepositoryPort reportesRepositoryPort;
    @Mock
    private SemilleroRepositoryPort semilleroRepositoryPort;
    @Mock
    private ExportadorReportePort exportadorReportePort;

    private ConsultarReportesUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarReportesUseCaseImpl(reportesRepositoryPort, semilleroRepositoryPort,
                exportadorReportePort, CLOCK);
    }

    private static ReporteFiltro filtro(String periodo) {
        return ReporteFiltro.de(periodo, null, null, null, null);
    }

    private void simularConteos(String periodo, long semilleros, long registrados, long activos) {
        when(reportesRepositoryPort.contarSemillerosActivos(argThat(f -> f != null && Objects.equals(f.periodo(), periodo))))
                .thenReturn(semilleros);
        when(reportesRepositoryPort.contarIntegrantesRegistrados(argThat(f -> f != null && Objects.equals(f.periodo(), periodo))))
                .thenReturn(registrados);
        when(reportesRepositoryPort.contarIntegrantesActivos(argThat(f -> f != null && Objects.equals(f.periodo(), periodo))))
                .thenReturn(activos);
    }

    @Test
    @DisplayName("obtenerKpis: calcula tasa de participación (RN3) y variaciones frente al período anterior (RN4)")
    void obtenerKpis_calculaTasaYTendencias() {
        simularConteos("2025", 12, 200, 150);
        simularConteos("2024", 10, 160, 120);
        when(reportesRepositoryPort.contarActividadesRealizadas(argThat(f -> f != null && "2025".equals(f.periodo())))).thenReturn(40L);
        when(reportesRepositoryPort.contarActividadesRealizadas(argThat(f -> f != null && "2024".equals(f.periodo())))).thenReturn(32L);

        ReporteKpis kpis = useCase.obtenerKpis(filtro("2025"));

        assertThat(kpis.semillerosActivos()).isEqualTo(12);
        assertThat(kpis.usuariosRegistrados()).isEqualTo(200);
        assertThat(kpis.miembrosActivos()).isEqualTo(150);
        assertThat(kpis.actividadesRealizadas()).isEqualTo(40);
        assertThat(kpis.tasaParticipacion()).isEqualTo(75.0);
        assertThat(kpis.tendencias().semillerosActivos()).isEqualTo(20.0);
        assertThat(kpis.tendencias().usuariosRegistrados()).isEqualTo(25.0);
        assertThat(kpis.tendencias().miembrosActivos()).isEqualTo(25.0);
        assertThat(kpis.tendencias().actividadesRealizadas()).isEqualTo(25.0);
        assertThat(kpis.tendencias().tasaParticipacion()).isEqualTo(0.0);
        assertThat(kpis.periodoComparado()).isEqualTo("2024");
        assertThat(kpis.alcance()).isEqualTo(AlcanceReporte.ADMIN);
        assertThat(kpis.fechaCalculo()).isEqualTo("2026-09-24T10:30");
    }

    @Test
    @DisplayName("obtenerKpis: sin datos previos ni integrantes, tasa y tendencias son null (no cero)")
    void obtenerKpis_sinBaseDeComparacion() {
        simularConteos(null, 3, 0, 0);

        ReporteKpis kpis = useCase.obtenerKpis(filtro(null));

        assertThat(kpis.tasaParticipacion()).isNull();
        assertThat(kpis.tendencias().semillerosActivos()).isEqualTo(0.0);
        assertThat(kpis.tendencias().usuariosRegistrados()).isNull();
        assertThat(kpis.tendencias().tasaParticipacion()).isNull();
        assertThat(kpis.periodoComparado()).isEqualTo("mes anterior");
    }

    @Test
    @DisplayName("obtenerKpis: el público no puede consultar un semillero específico (RN44)")
    void obtenerKpis_publicoConSemillero_lanzaAccesoNoAutorizado() {
        ReporteFiltro publico = ReporteFiltro.de(null, null, null, null, 7L).paraPublico();

        assertThatThrownBy(() -> useCase.obtenerKpis(publico)).isInstanceOf(AccesoNoAutorizadoException.class);
        verifyNoInteractions(reportesRepositoryPort);
    }

    @Test
    @DisplayName("obtenerKpis: un coordinador no puede consultar semilleros de otro coordinador (RN43/RN45)")
    void obtenerKpis_coordinadorSemilleroAjeno_lanzaAccesoNoAutorizado() {
        ReporteFiltro filtro = ReporteFiltro.de(null, null, null, null, 7L).paraCoordinador(5L);
        when(semilleroRepositoryPort.buscarPorId(7L))
                .thenReturn(Optional.of(Semillero.builder().id(7L).idCoordinador(99L).build()));

        assertThatThrownBy(() -> useCase.obtenerKpis(filtro)).isInstanceOf(AccesoNoAutorizadoException.class);
    }

    @Test
    @DisplayName("obtenerKpis: un coordinador no puede consultar un semillero inexistente")
    void obtenerKpis_coordinadorSemilleroInexistente_lanzaAccesoNoAutorizado() {
        ReporteFiltro filtro = ReporteFiltro.de(null, null, null, null, 7L).paraCoordinador(5L);
        when(semilleroRepositoryPort.buscarPorId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.obtenerKpis(filtro)).isInstanceOf(AccesoNoAutorizadoException.class);
    }

    @Test
    @DisplayName("obtenerKpis: un coordinador consulta su propio semillero")
    void obtenerKpis_coordinadorSemilleroPropio_consulta() {
        ReporteFiltro filtro = ReporteFiltro.de(null, null, null, null, 7L).paraCoordinador(5L);
        when(semilleroRepositoryPort.buscarPorId(7L))
                .thenReturn(Optional.of(Semillero.builder().id(7L).idCoordinador(5L).build()));

        ReporteKpis kpis = useCase.obtenerKpis(filtro);

        assertThat(kpis.alcance()).isEqualTo(AlcanceReporte.COORDINADOR);
    }

    @Test
    @DisplayName("obtenerDashboard: reúne todos los agregados con los mismos filtros")
    void obtenerDashboard_reuneAgregados() {
        ReporteFiltro filtro = filtro("2026");
        List<ReporteUnidad> unidades = List.of(new ReporteUnidad(1L, "Facultad de Artes", TipoUnidad.FACULTAD, 3, 20));
        List<ReporteConteo> campus = List.of(new ReporteConteo("1", "Medellín", 3));
        List<ReporteConteo> sexo = List.of(new ReporteConteo("FEMENINO", "Femenino", 12));
        List<ReporteConteo> roles = List.of(new ReporteConteo("TUTOR", "Tutor", 2));
        List<ReporteConteo> actividades = List.of(new ReporteConteo("4", "Talleres", 5));
        when(reportesRepositoryPort.distribucionPorUnidad(filtro)).thenReturn(unidades);
        when(reportesRepositoryPort.distribucionPorCampus(filtro)).thenReturn(campus);
        when(reportesRepositoryPort.integrantesPorSexo(filtro)).thenReturn(sexo);
        when(reportesRepositoryPort.integrantesPorRol(filtro)).thenReturn(roles);
        when(reportesRepositoryPort.actividadesPorTipo(filtro)).thenReturn(actividades);
        when(reportesRepositoryPort.semillerosCreadosPorAnio(any())).thenReturn(Map.of(2025, 2L, 2026, 1L));
        ReporteAsistencia asistencia = new ReporteAsistencia(4, new ConteoAsistencia(30, 10, 2));
        when(reportesRepositoryPort.asistencia(filtro)).thenReturn(asistencia);

        ReporteDashboard dashboard = useCase.obtenerDashboard(filtro);

        assertThat(dashboard.porUnidad()).isEqualTo(unidades);
        assertThat(dashboard.porCampus()).isEqualTo(campus);
        assertThat(dashboard.topFacultades()).isEqualTo(unidades);
        assertThat(dashboard.porSexo()).isEqualTo(sexo);
        assertThat(dashboard.porRol()).isEqualTo(roles);
        assertThat(dashboard.actividadesPorTipo()).isEqualTo(actividades);
        assertThat(dashboard.asistencia()).isEqualTo(asistencia);
        assertThat(dashboard.evolucion()).extracting(ReporteEvolucion::anio).containsExactly(2025, 2026);
        assertThat(dashboard.kpis()).isNotNull();
    }

    @Test
    @DisplayName("topFacultades: solo facultades con semilleros, ordenadas, e incluye empates en el quinto puesto (RN21)")
    void topFacultades_incluyeEmpates() {
        List<ReporteUnidad> unidades = List.of(
                new ReporteUnidad(1L, "Facultad A", TipoUnidad.FACULTAD, 9, 0),
                new ReporteUnidad(2L, "Facultad B", TipoUnidad.FACULTAD, 7, 0),
                new ReporteUnidad(3L, "Escuela C", TipoUnidad.ESCUELA, 50, 0),
                new ReporteUnidad(4L, "Facultad D", TipoUnidad.FACULTAD, 5, 0),
                new ReporteUnidad(5L, "Facultad E", TipoUnidad.FACULTAD, 4, 0),
                new ReporteUnidad(6L, "Facultad F", TipoUnidad.FACULTAD, 3, 0),
                new ReporteUnidad(7L, "Facultad G", TipoUnidad.FACULTAD, 3, 0),
                new ReporteUnidad(8L, "Facultad H", TipoUnidad.FACULTAD, 1, 0),
                new ReporteUnidad(9L, "Facultad I", TipoUnidad.FACULTAD, 0, 0));

        List<ReporteUnidad> top = useCase.topFacultades(unidades);

        assertThat(top).extracting(ReporteUnidad::nombre)
                .containsExactly("Facultad A", "Facultad B", "Facultad D", "Facultad E", "Facultad F", "Facultad G");
    }

    @Test
    @DisplayName("evolucion: acumula desde el primer año, rellena años sin creaciones y proyecta el siguiente (RN24/RN27)")
    void evolucion_acumulaYProyecta() {
        when(reportesRepositoryPort.semillerosCreadosPorAnio(any())).thenReturn(Map.of(2022, 2L, 2024, 3L, 2026, 1L));

        List<ReporteEvolucion> puntos = useCase.evolucion(filtro(null));

        assertThat(puntos).containsExactly(
                new ReporteEvolucion(2022, 2, 2, false),
                new ReporteEvolucion(2023, 2, 0, false),
                new ReporteEvolucion(2024, 5, 3, false),
                new ReporteEvolucion(2025, 5, 0, false),
                new ReporteEvolucion(2026, 6, 1, false),
                new ReporteEvolucion(2027, 7, 1, true));
    }

    @Test
    @DisplayName("evolucion: con período termina en el año de corte e ignora el período al consultar el histórico")
    void evolucion_conPeriodo_terminaEnAnioDeCorte() {
        when(reportesRepositoryPort.semillerosCreadosPorAnio(argThat(f -> f != null && f.periodo() == null)))
                .thenReturn(Map.of(2023, 1L, 2024, 1L));

        List<ReporteEvolucion> puntos = useCase.evolucion(filtro("2024-1"));

        assertThat(puntos).extracting(ReporteEvolucion::anio).containsExactly(2023, 2024);
        assertThat(puntos).noneMatch(ReporteEvolucion::proyectado);
    }

    @Test
    @DisplayName("evolucion: sin semilleros con año de creación retorna lista vacía")
    void evolucion_sinDatos() {
        when(reportesRepositoryPort.semillerosCreadosPorAnio(any())).thenReturn(Map.of());

        assertThat(useCase.evolucion(filtro(null))).isEmpty();
    }

    @Test
    @DisplayName("evolucion: la proyección nunca decrece respecto al último año")
    void evolucion_proyeccionNoDecrece() {
        when(reportesRepositoryPort.semillerosCreadosPorAnio(any())).thenReturn(Map.of(2024, 10L));

        List<ReporteEvolucion> puntos = useCase.evolucion(filtro(null));

        assertThat(puntos.get(puntos.size() - 1)).isEqualTo(new ReporteEvolucion(2027, 10, 0, true));
    }

    @Test
    @DisplayName("obtenerRendimiento: normaliza página y tamaño antes de consultar")
    void obtenerRendimiento_normalizaPaginacion() {
        ReporteFiltro filtro = filtro(null);
        PageResult<ReporteRendimiento> pagina = PageResult.<ReporteRendimiento>builder().contenido(List.of()).build();
        when(reportesRepositoryPort.rendimiento(filtro, 0, 100, OrdenRendimiento.PARTICIPANTES, false)).thenReturn(pagina);

        assertThat(useCase.obtenerRendimiento(filtro, -3, 5000, OrdenRendimiento.PARTICIPANTES, false)).isSameAs(pagina);
    }

    @Test
    @DisplayName("obtenerRendimiento: el público no accede a la tabla por semillero (RN44)")
    void obtenerRendimiento_publico_lanzaAccesoNoAutorizado() {
        ReporteFiltro publico = filtro(null).paraPublico();

        assertThatThrownBy(() -> useCase.obtenerRendimiento(publico, 0, 10, OrdenRendimiento.NOMBRE, true))
                .isInstanceOf(AccesoNoAutorizadoException.class);
        verify(reportesRepositoryPort, org.mockito.Mockito.never()).rendimiento(any(), anyInt(), anyInt(), any(), eq(true));
    }

    @Test
    @DisplayName("listarSemilleros: lista los activos ignorando el semillero seleccionado")
    void listarSemilleros_ignoraSemilleroSeleccionado() {
        List<ReporteOpcion> opciones = List.of(new ReporteOpcion(1L, "Semillero IA"));
        when(reportesRepositoryPort.semillerosActivos(argThat(f -> f != null && f.idSemillero() == null))).thenReturn(opciones);

        assertThat(useCase.listarSemilleros(ReporteFiltro.de(null, null, null, null, 4L))).isEqualTo(opciones);
    }

    @Test
    @DisplayName("listarSemilleros: el público no puede listar semilleros para filtrar")
    void listarSemilleros_publico_lanzaAccesoNoAutorizado() {
        ReporteFiltro publico = filtro(null).paraPublico();

        assertThatThrownBy(() -> useCase.listarSemilleros(publico)).isInstanceOf(AccesoNoAutorizadoException.class);
    }

    @Test
    @DisplayName("exportar: genera el archivo con nombre reporte_sigsi_AAAA-MM-DD_HHMM (RN36) y filtros aplicados (RN35)")
    void exportar_generaArchivoConNombreConFecha() {
        ReporteFiltro filtro = ReporteFiltro.de("2026", "FACULTAD", null, null, null);
        List<ReporteRendimiento> filas = List.of(new ReporteRendimiento(1L, "Semillero IA", "SEM-1",
                "Facultad de Ingeniería", TipoUnidad.FACULTAD, "Medellín", 10, 4, 6, 75.0, "ACTIVO"));
        when(reportesRepositoryPort.rendimientoCompleto(filtro, OrdenRendimiento.NOMBRE, true)).thenReturn(filas);
        when(reportesRepositoryPort.semillerosCreadosPorAnio(any())).thenReturn(Map.of());
        when(exportadorReportePort.exportar(any(), eq(FormatoExportacion.XLSX))).thenReturn(new byte[]{1, 2, 3});

        ReporteArchivo archivo = useCase.exportar(filtro, FormatoExportacion.XLSX, OrdenRendimiento.NOMBRE, true);

        assertThat(archivo.nombre()).isEqualTo("reporte_sigsi_2026-09-24_1030.xlsx");
        assertThat(archivo.contentType()).isEqualTo(FormatoExportacion.XLSX.getContentType());
        assertThat(archivo.contenido()).containsExactly(1, 2, 3);
        ArgumentCaptor<ReporteCompleto> captor = ArgumentCaptor.forClass(ReporteCompleto.class);
        verify(exportadorReportePort).exportar(captor.capture(), eq(FormatoExportacion.XLSX));
        assertThat(captor.getValue().filtro()).isEqualTo(filtro);
        assertThat(captor.getValue().rendimiento()).isEqualTo(filas);
    }

    @Test
    @DisplayName("huellaDatos: delega en el repositorio")
    void huellaDatos_delega() {
        when(reportesRepositoryPort.huellaDatos()).thenReturn("abc");

        assertThat(useCase.huellaDatos()).isEqualTo("abc");
    }

    @Test
    @DisplayName("porcentaje y variacion: redondean a un decimal y evitan divisiones por cero")
    void porcentajeYVariacion() {
        assertThat(ConsultarReportesUseCaseImpl.porcentaje(1, 3)).isEqualTo(33.3);
        assertThat(ConsultarReportesUseCaseImpl.porcentaje(1, 0)).isNull();
        assertThat(ConsultarReportesUseCaseImpl.variacion(5, 4)).isEqualTo(25.0);
        assertThat(ConsultarReportesUseCaseImpl.variacion(3, 4)).isEqualTo(-25.0);
        assertThat(ConsultarReportesUseCaseImpl.variacion(3, 0)).isNull();
    }
}
