package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.reporte.FormatoExportacion;
import co.udea.semilleros.domain.model.reporte.OrdenRendimiento;
import co.udea.semilleros.domain.model.reporte.ReporteArchivo;
import co.udea.semilleros.domain.model.reporte.ReporteDashboard;
import co.udea.semilleros.domain.model.reporte.ReporteFiltro;
import co.udea.semilleros.domain.model.reporte.ReporteKpis;
import co.udea.semilleros.domain.model.reporte.ReporteOpcion;
import co.udea.semilleros.domain.model.reporte.ReporteRendimiento;
import co.udea.semilleros.domain.port.in.ConsultarReportesUseCase;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.request.ReporteFiltroRequest;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.PageResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.sse.ReportesEventosPublisher;
import co.udea.semilleros.infrastructure.security.filter.CoordinadorPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Reportes y estadísticas (HU1-HU14). Requieren sesión y el alcance depende del rol (HU12):
 * <ul>
 *     <li>{@code /api/v1/admin/reportes}: administrador, datos globales y exportación</li>
 *     <li>{@code /api/v1/coordinador/reportes}: coordinador, solo sus semilleros</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Reportes y estadísticas", description = "Indicadores, gráficos, tabla de rendimiento y exportación")
public class ReportesController {

    private static final String ADMIN = "/api/v1/admin/reportes";
    private static final String COORDINADOR = "/api/v1/coordinador/reportes";

    private final ConsultarReportesUseCase consultarReportesUseCase;
    private final ReportesEventosPublisher reportesEventosPublisher;

    // ─── Administrador ──────────────────────────────────────────────────────────

    @GetMapping(ADMIN + "/kpis")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "KPIs globales", description = "Semilleros activos, usuarios registrados, actividades "
            + "realizadas y tasa de participación con su variación frente al período anterior.")
    public ResponseEntity<ApiResponse<ReporteKpis>> kpisAdmin(@ModelAttribute ReporteFiltroRequest filtro) {
        return ResponseEntity.ok(ApiResponse.exito(consultarReportesUseCase.obtenerKpis(filtro.toFiltro())));
    }

    @GetMapping(ADMIN + "/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Tablero completo", description = "KPIs, distribución por unidad y campus, top 5 de "
            + "facultades, composición por sexo y rol, evolución anual y actividades por tipo.")
    public ResponseEntity<ApiResponse<ReporteDashboard>> dashboardAdmin(@ModelAttribute ReporteFiltroRequest filtro) {
        return ResponseEntity.ok(ApiResponse.exito(consultarReportesUseCase.obtenerDashboard(filtro.toFiltro())));
    }

    @GetMapping(ADMIN + "/rendimiento")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Tabla de rendimiento por semillero", description = "Paginada y ordenable por "
            + "nombre, unidad, tipo, campus, participantes, actividades o estado.")
    public ResponseEntity<ApiResponse<PageResponse<ReporteRendimiento>>> rendimientoAdmin(
            @ModelAttribute ReporteFiltroRequest filtro,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "15") int tamano,
            @Parameter(description = "nombre, unidad, tipo, campus, participantes, actividades o estado")
            @RequestParam(required = false) String orden,
            @Parameter(description = "asc o desc") @RequestParam(defaultValue = "asc") String direccion
    ) {
        return rendimiento(filtro.toFiltro(), pagina, tamano, orden, direccion);
    }

    @GetMapping(ADMIN + "/semilleros")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Semilleros activos para el filtro", description = "Lista de semilleros activos que "
            + "cumplen los demás filtros (unidad, campus, tipo, período).")
    public ResponseEntity<ApiResponse<List<ReporteOpcion>>> semillerosAdmin(@ModelAttribute ReporteFiltroRequest filtro) {
        return ResponseEntity.ok(ApiResponse.exito(consultarReportesUseCase.listarSemilleros(filtro.toFiltro())));
    }

    @GetMapping(ADMIN + "/exportar")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Exportar reporte", description = "Genera el reporte con los filtros aplicados en xlsx, "
            + "pdf o csv. El archivo se nombra reporte_sigsi_AAAA-MM-DD_HHMM.")
    public ResponseEntity<byte[]> exportarAdmin(
            @ModelAttribute ReporteFiltroRequest filtro,
            @Parameter(description = "xlsx, pdf o csv") @RequestParam String formato,
            @RequestParam(required = false) String orden,
            @RequestParam(defaultValue = "asc") String direccion
    ) {
        ReporteArchivo archivo = consultarReportesUseCase.exportar(filtro.toFiltro(), FormatoExportacion.de(formato),
                OrdenRendimiento.de(orden), esAscendente(direccion));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
                .body(archivo.contenido());
    }

    @GetMapping(value = ADMIN + "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Eventos de actualización (SSE)", description = "Flujo Server-Sent Events que emite "
            + "'datos-actualizados' cuando cambian los datos de los reportes.")
    public SseEmitter eventosAdmin() {
        return reportesEventosPublisher.suscribir();
    }

    // ─── Coordinador: solo sus semilleros (RN43) ────────────────────────────────

    @GetMapping(COORDINADOR + "/dashboard")
    @PreAuthorize("hasRole('COORDINADOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Tablero del coordinador", description = "Mismos indicadores del tablero, calculados "
            + "solo sobre los semilleros del coordinador autenticado.")
    public ResponseEntity<ApiResponse<ReporteDashboard>> dashboardCoordinador(
            @ModelAttribute ReporteFiltroRequest filtro,
            @AuthenticationPrincipal CoordinadorPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.exito(
                consultarReportesUseCase.obtenerDashboard(filtro.toFiltro().paraCoordinador(principal.getId()))));
    }

    @GetMapping(COORDINADOR + "/rendimiento")
    @PreAuthorize("hasRole('COORDINADOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Rendimiento de los semilleros del coordinador")
    public ResponseEntity<ApiResponse<PageResponse<ReporteRendimiento>>> rendimientoCoordinador(
            @ModelAttribute ReporteFiltroRequest filtro,
            @AuthenticationPrincipal CoordinadorPrincipal principal,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "15") int tamano,
            @RequestParam(required = false) String orden,
            @RequestParam(defaultValue = "asc") String direccion
    ) {
        return rendimiento(filtro.toFiltro().paraCoordinador(principal.getId()), pagina, tamano, orden, direccion);
    }

    @GetMapping(COORDINADOR + "/semilleros")
    @PreAuthorize("hasRole('COORDINADOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Semilleros activos del coordinador para el filtro")
    public ResponseEntity<ApiResponse<List<ReporteOpcion>>> semillerosCoordinador(
            @ModelAttribute ReporteFiltroRequest filtro,
            @AuthenticationPrincipal CoordinadorPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.exito(
                consultarReportesUseCase.listarSemilleros(filtro.toFiltro().paraCoordinador(principal.getId()))));
    }

    private ResponseEntity<ApiResponse<PageResponse<ReporteRendimiento>>> rendimiento(
            ReporteFiltro filtro, int pagina, int tamano, String orden, String direccion) {
        PageResult<ReporteRendimiento> resultado = consultarReportesUseCase.obtenerRendimiento(
                filtro, pagina, tamano, OrdenRendimiento.de(orden), esAscendente(direccion));
        return ResponseEntity.ok(ApiResponse.exito(aPageResponse(resultado)));
    }

    private static boolean esAscendente(String direccion) {
        return !"desc".equalsIgnoreCase(direccion);
    }

    private static PageResponse<ReporteRendimiento> aPageResponse(PageResult<ReporteRendimiento> pagina) {
        return PageResponse.<ReporteRendimiento>builder()
                .contenido(pagina.getContenido())
                .paginaActual(pagina.getPaginaActual())
                .tamano(pagina.getTamano())
                .totalElementos(pagina.getTotalElementos())
                .totalPaginas(pagina.getTotalPaginas())
                .esPrimeraPagina(pagina.isEsPrimeraPagina())
                .esUltimaPagina(pagina.isEsUltimaPagina())
                .build();
    }
}
