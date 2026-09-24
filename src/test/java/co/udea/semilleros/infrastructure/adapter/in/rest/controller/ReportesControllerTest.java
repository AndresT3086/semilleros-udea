package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.exception.AccesoNoAutorizadoException;
import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.asistencia.ConteoAsistencia;
import co.udea.semilleros.domain.model.reporte.AlcanceReporte;
import co.udea.semilleros.domain.model.reporte.FormatoExportacion;
import co.udea.semilleros.domain.model.reporte.OrdenRendimiento;
import co.udea.semilleros.domain.model.reporte.ReporteArchivo;
import co.udea.semilleros.domain.model.reporte.ReporteAsistencia;
import co.udea.semilleros.domain.model.reporte.ReporteDashboard;
import co.udea.semilleros.domain.model.reporte.ReporteFiltro;
import co.udea.semilleros.domain.model.reporte.ReporteKpis;
import co.udea.semilleros.domain.model.reporte.ReporteOpcion;
import co.udea.semilleros.domain.model.reporte.ReporteRendimiento;
import co.udea.semilleros.domain.model.reporte.TipoUnidad;
import co.udea.semilleros.domain.port.in.ConsultarReportesUseCase;
import co.udea.semilleros.infrastructure.adapter.in.rest.sse.ReportesEventosPublisher;
import co.udea.semilleros.infrastructure.config.GlobalExceptionHandler;
import co.udea.semilleros.infrastructure.security.filter.CoordinadorPrincipal;
import co.udea.semilleros.infrastructure.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ReportesControllerTest.SeguridadPorMetodo.class})
@DisplayName("ReportesController - Pruebas de capa web")
class ReportesControllerTest {

    /** Activa @PreAuthorize en el slice web para verificar las restricciones por rol. */
    @TestConfiguration
    @EnableMethodSecurity
    static class SeguridadPorMetodo {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultarReportesUseCase consultarReportesUseCase;

    @MockBean
    private ReportesEventosPublisher reportesEventosPublisher;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final ReporteKpis KPIS = new ReporteKpis(4, 10, 8, 12, 80.0,
            new ReporteKpis.Tendencias(33.3, null, null, null), "2024",
            LocalDateTime.of(2026, 9, 24, 10, 30), AlcanceReporte.ADMIN);

    private static final ReporteDashboard DASHBOARD = new ReporteDashboard(KPIS, List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of(), new ReporteAsistencia(0, ConteoAsistencia.VACIO));

    private static void autenticarComo(String rol, Long id) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CoordinadorPrincipal(id, "usuario@udea.edu.co", rol), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /admin/reportes/kpis: traduce los query params en filtros (RN5)")
    void kpisAdmin_retornaKpis() throws Exception {
        autenticarComo("ADMIN", 1L);
        when(consultarReportesUseCase.obtenerKpis(argThat(f -> f != null
                && "2025-1".equals(f.periodo()) && f.tipoUnidad() == TipoUnidad.FACULTAD
                && Long.valueOf(3L).equals(f.idCampus()) && f.alcance() == AlcanceReporte.ADMIN)))
                .thenReturn(KPIS);

        mockMvc.perform(get("/api/v1/admin/reportes/kpis")
                        .param("periodo", "2025-1").param("tipoUnidad", "FACULTAD").param("idCampus", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.semillerosActivos").value(4))
                .andExpect(jsonPath("$.datos.tasaParticipacion").value(80.0))
                .andExpect(jsonPath("$.datos.tendencias.semillerosActivos").value(33.3))
                .andExpect(jsonPath("$.datos.periodoComparado").value("2024"));
    }

    @Test
    @DisplayName("GET /admin/reportes/kpis: período mal formado retorna 400")
    void kpisAdmin_periodoInvalido_retorna400() throws Exception {
        autenticarComo("ADMIN", 1L);

        mockMvc.perform(get("/api/v1/admin/reportes/kpis").param("periodo", "2025-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoError").value("FILTRO_REPORTE_INVALIDO"));
    }

    @Test
    @DisplayName("GET /admin/reportes/dashboard: un coordinador recibe 403 (RN45)")
    void dashboardAdmin_coordinador_retorna403() throws Exception {
        autenticarComo("COORDINADOR", 5L);

        mockMvc.perform(get("/api/v1/admin/reportes/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigoError").value("ACCESO_NO_AUTORIZADO"));
        verifyNoInteractions(consultarReportesUseCase);
    }

    @Test
    @DisplayName("GET /admin/reportes/dashboard: retorna el tablero completo")
    void dashboardAdmin_retornaTablero() throws Exception {
        autenticarComo("ADMIN", 1L);
        when(consultarReportesUseCase.obtenerDashboard(any())).thenReturn(DASHBOARD);

        mockMvc.perform(get("/api/v1/admin/reportes/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.kpis.usuariosRegistrados").value(10))
                .andExpect(jsonPath("$.datos.porUnidad").isArray());
    }

    @Test
    @DisplayName("GET /admin/reportes/rendimiento: pagina y ordena según los parámetros")
    void rendimientoAdmin_paginado() throws Exception {
        autenticarComo("ADMIN", 1L);
        ReporteRendimiento fila = new ReporteRendimiento(1L, "Semillero IA", "SEM-1", "Facultad de Ingeniería",
                TipoUnidad.FACULTAD, "Medellín", 10, 4, 6, 83.3, "ACTIVO");
        when(consultarReportesUseCase.obtenerRendimiento(any(), eq(1), eq(5), eq(OrdenRendimiento.PARTICIPANTES), eq(false)))
                .thenReturn(PageResult.<ReporteRendimiento>builder().contenido(List.of(fila)).paginaActual(1).tamano(5)
                        .totalElementos(6).totalPaginas(2).esUltimaPagina(true).build());

        mockMvc.perform(get("/api/v1/admin/reportes/rendimiento")
                        .param("pagina", "1").param("tamano", "5")
                        .param("orden", "participantes").param("direccion", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.contenido[0].nombre").value("Semillero IA"))
                .andExpect(jsonPath("$.datos.contenido[0].tipoUnidad").value("FACULTAD"))
                .andExpect(jsonPath("$.datos.contenido[0].porcentajeAsistencia").value(83.3))
                .andExpect(jsonPath("$.datos.totalElementos").value(6))
                .andExpect(jsonPath("$.datos.esUltimaPagina").value(true));
    }

    @Test
    @DisplayName("GET /admin/reportes/rendimiento: columna de orden desconocida retorna 400")
    void rendimientoAdmin_ordenInvalido_retorna400() throws Exception {
        autenticarComo("ADMIN", 1L);

        mockMvc.perform(get("/api/v1/admin/reportes/rendimiento").param("orden", "cedula"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /admin/reportes/semilleros: lista semilleros activos")
    void semillerosAdmin_lista() throws Exception {
        autenticarComo("ADMIN", 1L);
        when(consultarReportesUseCase.listarSemilleros(any())).thenReturn(List.of(new ReporteOpcion(1L, "Semillero IA")));

        mockMvc.perform(get("/api/v1/admin/reportes/semilleros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].nombre").value("Semillero IA"));
    }

    @Test
    @DisplayName("GET /admin/reportes/exportar: descarga el archivo con su nombre (RN36)")
    void exportarAdmin_descargaArchivo() throws Exception {
        autenticarComo("ADMIN", 1L);
        when(consultarReportesUseCase.exportar(any(ReporteFiltro.class), eq(FormatoExportacion.CSV),
                eq(OrdenRendimiento.NOMBRE), eq(true)))
                .thenReturn(new ReporteArchivo("reporte_sigsi_2026-09-24_1030.csv", "text/csv;charset=UTF-8",
                        "a;b".getBytes()));

        mockMvc.perform(get("/api/v1/admin/reportes/exportar").param("formato", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"reporte_sigsi_2026-09-24_1030.csv\""))
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().bytes("a;b".getBytes()));
    }

    @Test
    @DisplayName("GET /admin/reportes/exportar: formato no soportado retorna 400")
    void exportarAdmin_formatoInvalido_retorna400() throws Exception {
        autenticarComo("ADMIN", 1L);

        mockMvc.perform(get("/api/v1/admin/reportes/exportar").param("formato", "docx"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /admin/reportes/eventos: abre el flujo SSE para el administrador (HU13)")
    void eventosAdmin_suscribe() throws Exception {
        autenticarComo("ADMIN", 1L);
        when(reportesEventosPublisher.suscribir()).thenReturn(new SseEmitter());

        mockMvc.perform(get("/api/v1/admin/reportes/eventos").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    @DisplayName("GET /coordinador/reportes/dashboard: limita el alcance al coordinador autenticado (RN43)")
    void dashboardCoordinador_usaAlcanceDelCoordinador() throws Exception {
        autenticarComo("COORDINADOR", 5L);
        when(consultarReportesUseCase.obtenerDashboard(argThat(f -> f != null
                && f.alcance() == AlcanceReporte.COORDINADOR && Long.valueOf(5L).equals(f.idCoordinador()))))
                .thenReturn(DASHBOARD);

        mockMvc.perform(get("/api/v1/coordinador/reportes/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));
    }

    @Test
    @DisplayName("GET /coordinador/reportes/dashboard: semillero ajeno retorna 403")
    void dashboardCoordinador_semilleroAjeno_retorna403() throws Exception {
        autenticarComo("COORDINADOR", 5L);
        when(consultarReportesUseCase.obtenerDashboard(any()))
                .thenThrow(new AccesoNoAutorizadoException("reporte del semillero 9"));

        mockMvc.perform(get("/api/v1/coordinador/reportes/dashboard").param("idSemillero", "9"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /coordinador/reportes/rendimiento y /semilleros: usan el alcance del coordinador")
    void rendimientoYSemillerosCoordinador() throws Exception {
        autenticarComo("COORDINADOR", 5L);
        when(consultarReportesUseCase.obtenerRendimiento(argThat(f -> f != null && Long.valueOf(5L).equals(f.idCoordinador())),
                eq(0), eq(15), eq(OrdenRendimiento.NOMBRE), eq(true)))
                .thenReturn(PageResult.<ReporteRendimiento>builder().contenido(List.of()).build());
        when(consultarReportesUseCase.listarSemilleros(argThat(f -> f != null && Long.valueOf(5L).equals(f.idCoordinador()))))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/coordinador/reportes/rendimiento")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/coordinador/reportes/semilleros")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /coordinador/reportes/dashboard: un administrador recibe 403")
    void dashboardCoordinador_admin_retorna403() throws Exception {
        autenticarComo("ADMIN", 1L);

        mockMvc.perform(get("/api/v1/coordinador/reportes/dashboard")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /reportes/publico/dashboard: consulta agregada con alcance público (RN44)")
    void dashboardPublico_usaAlcancePublico() throws Exception {
        when(consultarReportesUseCase.obtenerDashboard(argThat(f -> f != null && f.alcance() == AlcanceReporte.PUBLICO)))
                .thenReturn(DASHBOARD);

        mockMvc.perform(get("/api/v1/reportes/publico/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.kpis.semillerosActivos").value(4));
    }
}
