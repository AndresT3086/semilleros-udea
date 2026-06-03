package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.port.in.GestionarSemilleroUseCase;
import co.udea.semilleros.domain.port.out.ActividadesRepositoryPort;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.request.*;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.*;
import co.udea.semilleros.infrastructure.adapter.in.rest.mapper.SemilleroRestMapper;
import co.udea.semilleros.infrastructure.security.filter.CoordinadorPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coordinador/semilleros")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COORDINADOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Coordinador - Gestión de Semilleros",
     description = "Endpoints protegidos para coordinadores: creación y caracterización de semilleros por pestañas")
public class CoordinadorSemilleroController {

    private final GestionarSemilleroUseCase gestionarSemilleroUseCase;
    private final SemilleroRestMapper semilleroRestMapper;

    @PostMapping("/iniciar")
    @Operation(
        summary = "Iniciar caracterización de semillero",
        description = "Crea un semillero en estado BORRADOR con código único generado automáticamente. "
                    + "Permite al coordinador registrar un nuevo semillero para iniciar su caracterización."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Semillero borrador creado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<ApiResponse<SemilleroDetalleResponse>> iniciarCaracterizacion(
            @AuthenticationPrincipal CoordinadorPrincipal principal
    ) {
        Semillero semillero = gestionarSemilleroUseCase.crearSemilleroBorrador(principal.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.exito("Semillero iniciado. Código asignado: " + semillero.getCodigo(),
                        semilleroRestMapper.toDetalleResponse(semillero)));
    }

    @GetMapping("/mis-semilleros")
    @Operation(
        summary = "Obtener semilleros del coordinador autenticado",
        description = "Retorna el semillero asociado al coordinador autenticado con el progreso de caracterización."
    )
    public ResponseEntity<ApiResponse<List<SemilleroDetalleResponse>>> obtenerMisSemilleros(
            @AuthenticationPrincipal CoordinadorPrincipal principal
    ) {
        List<SemilleroDetalleResponse> response = gestionarSemilleroUseCase
                .obtenerSemillerosDelCoordinador(principal.getId())
                .stream()
                .map(semilleroRestMapper::toDetalleResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.exito(response));
    }

    @GetMapping("/{idSemillero}")
    @Operation(
            summary = "Obtener semillero específico del coordinador",
            description = "Retorna el detalle de un semillero específico validando que pertenece al coordinador autenticado."
    )
    public ResponseEntity<ApiResponse<SemilleroDetalleResponse>> obtenerSemillero(
            @PathVariable Long idSemillero,
            @AuthenticationPrincipal CoordinadorPrincipal principal
    ) {
        Semillero semillero = gestionarSemilleroUseCase
                .obtenerSemilleroDelCoordinadorPorId(idSemillero, principal.getId());

        return ResponseEntity.ok(ApiResponse.exito(semilleroRestMapper.toDetalleResponse(semillero)));
    }

    @GetMapping("/{idSemillero}/inscripciones/pendientes")
    @Operation(
            summary = "Listar solicitudes pendientes",
            description = "Retorna las solicitudes de inscripción pendientes de un semillero del coordinador autenticado."
    )
    public ResponseEntity<ApiResponse<List<InscripcionResponse>>> listarInscripcionesPendientes(
            @PathVariable Long idSemillero,
            @AuthenticationPrincipal CoordinadorPrincipal principal
    ) {
        List<InscripcionResponse> response = gestionarSemilleroUseCase
                .listarInscripcionesPendientes(idSemillero, principal.getId())
                .stream()
                .map(semilleroRestMapper::toInscripcionResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.exito(response));
    }

    @PostMapping("/inscripciones/{idInscripcion}/aprobar")
    @Operation(
            summary = "Aprobar solicitud de inscripción",
            description = "Aprueba una solicitud pendiente y registra al estudiante como integrante del semillero."
    )
    public ResponseEntity<ApiResponse<InscripcionResponse>> aprobarInscripcion(
            @PathVariable Long idInscripcion,
            @AuthenticationPrincipal CoordinadorPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.exito(
                "Solicitud aprobada exitosamente.",
                semilleroRestMapper.toInscripcionResponse(
                        gestionarSemilleroUseCase.aprobarInscripcion(idInscripcion, principal.getId()))));
    }

    @PostMapping("/inscripciones/{idInscripcion}/rechazar")
    @Operation(
            summary = "Rechazar solicitud de inscripción",
            description = "Rechaza una solicitud pendiente de inscripción al semillero."
    )
    public ResponseEntity<ApiResponse<InscripcionResponse>> rechazarInscripcion(
            @PathVariable Long idInscripcion,
            @AuthenticationPrincipal CoordinadorPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.exito(
                "Solicitud rechazada exitosamente.",
                semilleroRestMapper.toInscripcionResponse(
                        gestionarSemilleroUseCase.rechazarInscripcion(idInscripcion, principal.getId()))));
    }

    @PatchMapping("/{idSemillero}/pestana/general")
    @Operation(
        summary = "Guardar pestaña General del formulario",
        description = "Guarda y valida los campos de la pestaña General. "
                    + "Todos los campos marcados con * son obligatorios para avanzar. "
                    + "Permite guardado parcial si el formulario se interrumpe."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Pestaña guardada exitosamente",
            content = @Content(schema = @Schema(implementation = SemilleroDetalleResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Campos obligatorios pendientes"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No tiene permisos sobre este semillero"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Semillero no encontrado")
    })
    public ResponseEntity<ApiResponse<SemilleroDetalleResponse>> guardarPestanaGeneral(
            @Parameter(description = "ID del semillero", required = true) @PathVariable Long idSemillero,
            @Valid @RequestBody GuardarPestanaGeneralRequest request,
            @AuthenticationPrincipal CoordinadorPrincipal principal
    ) {
        Semillero datos = semilleroRestMapper.toPestanaGeneralDomain(request);
        Semillero guardado = gestionarSemilleroUseCase.guardarPestanaGeneral(idSemillero, principal.getId(), datos);

        return ResponseEntity.ok(ApiResponse.exito(
                "Pestaña General guardada exitosamente.", semilleroRestMapper.toDetalleResponse(guardado)));
    }

    @PostMapping("/{idSemillero}/finalizar")
    @Operation(
        summary = "Finalizar caracterización del semillero",
        description = "Marca el semillero como CARACTERIZADO y notifica al administrador. "
                    + "Todas las pestañas deben estar completadas."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Semillero caracterizado exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No tiene permisos sobre este semillero"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Semillero no encontrado")
    })
    public ResponseEntity<ApiResponse<SemilleroDetalleResponse>> finalizarCaracterizacion(
            @Parameter(description = "ID del semillero", required = true) @PathVariable Long idSemillero,
            @AuthenticationPrincipal CoordinadorPrincipal principal
    ) {
        Semillero finalizado = gestionarSemilleroUseCase.finalizarCaracterizacion(idSemillero, principal.getId());
        return ResponseEntity.ok(ApiResponse.exito(
                "Semillero caracterizado exitosamente. Se ha notificado al administrador.",
                semilleroRestMapper.toDetalleResponse(finalizado)));
    }

    @PatchMapping("/{idSemillero}/pestana/produccion")
    @Operation(
            summary = "Guardar pestaña Producción Académica",
            description = "Guarda la información de producción académica del semillero.")
    public ResponseEntity<ApiResponse<SemilleroDetalleResponse>> guardarProduccion(
            @PathVariable Long idSemillero,
            @Valid @RequestBody GuardarPestanaProduccionRequest request,
            @AuthenticationPrincipal CoordinadorPrincipal principal) {

        Semillero guardado = gestionarSemilleroUseCase.guardarPestanaProduccion(
                idSemillero, principal.getId(),
                request.getTienenArticulos(),          request.getCantidadArticulos(),
                request.getTienenLibros(),             request.getCantidadLibros(),
                request.getOrganizanEventos(),         request.getCantidadEventosOrganizados(),
                request.getParticipaEnEventos(),       request.getCantidadParticipaciones()
        );

        return ResponseEntity.ok(ApiResponse.exito(
                "Pestaña Producción guardada.",
                semilleroRestMapper.toDetalleResponse(guardado)));
    }

    @PatchMapping("/{idSemillero}/pestana/organizacion")
    @Operation(summary = "Guardar pestaña Organización",
            description = "Guarda recursos y fuentes de financiación. Ambos campos son obligatorios.")
    public ResponseEntity<ApiResponse<SemilleroDetalleResponse>> guardarOrganizacion(
            @PathVariable Long idSemillero,
            @Valid @RequestBody GuardarPestanaOrganizacionRequest request,
            @AuthenticationPrincipal CoordinadorPrincipal principal) {

        Semillero guardado = gestionarSemilleroUseCase.guardarPestanaOrganizacion(
                idSemillero, principal.getId(),
                request.getIdsRecursos(), request.getIdsFuentesFinanciacion());

        return ResponseEntity.ok(ApiResponse.exito(
                "Pestaña Organización guardada.", semilleroRestMapper.toDetalleResponse(guardado)));
    }

    @PatchMapping("/{idSemillero}/pestana/relacionamiento")
    @Operation(summary = "Guardar pestaña Relacionamiento")
    public ResponseEntity<ApiResponse<SemilleroDetalleResponse>> guardarRelacionamiento(
            @PathVariable Long idSemillero,
            @RequestBody GuardarPestanaRelacionamientoRequest request,
            @AuthenticationPrincipal CoordinadorPrincipal principal) {

        Semillero guardado = gestionarSemilleroUseCase.guardarPestanaRelacionamiento(
                idSemillero, principal.getId(),
                request.getAdscritoGrupo(),
                request.getGrupoInvestigacion(),    request.getRelacionGrupo(),
                request.getCentroInvestigaciones(), request.getRelacionCentro(),
                request.getDepartamento(),          request.getRelacionDepartamento(),
                request.getFacultad(),              request.getRelacionFacultad()
        );

        return ResponseEntity.ok(ApiResponse.exito(
                "Pestaña Relacionamiento guardada.",
                semilleroRestMapper.toDetalleResponse(guardado)));
    }

    @PatchMapping("/{idSemillero}/pestana/actividades")
    @Operation(summary = "Guardar pestaña Actividades",
            description = "Guarda las actividades científicas que realiza el semillero (Sí/No por actividad).")
    public ResponseEntity<ApiResponse<SemilleroDetalleResponse>> guardarActividades(
            @PathVariable Long idSemillero,
            @Valid @RequestBody GuardarPestanaActividadesRequest request,
            @AuthenticationPrincipal CoordinadorPrincipal principal) {

        List<ActividadesRepositoryPort.ActividadDto> dtos = request.getActividades().stream()
                .map(a -> new ActividadesRepositoryPort.ActividadDto(a.getIdActividad(), a.getRealiza()))
                .toList();

        Semillero guardado = gestionarSemilleroUseCase
                .guardarPestanaActividades(idSemillero, principal.getId(), dtos);

        return ResponseEntity.ok(ApiResponse.exito(
                "Pestaña Actividades guardada.", semilleroRestMapper.toDetalleResponse(guardado)));
    }

    @PatchMapping("/{idSemillero}/pestana/dofa")
    @Operation(summary = "Guardar pestaña DOFA",
            description = "Guarda el análisis DOFA. Los cuatro campos son obligatorios.")
    public ResponseEntity<ApiResponse<SemilleroDetalleResponse>> guardarDofa(
            @PathVariable Long idSemillero,
            @Valid @RequestBody GuardarPestanaDofaRequest request,
            @AuthenticationPrincipal CoordinadorPrincipal principal) {

        Semillero guardado = gestionarSemilleroUseCase.guardarPestanaDofa(
                idSemillero, principal.getId(),
                request.getFortalezas(), request.getDebilidades(),
                request.getOportunidades(), request.getAmenazas());

        return ResponseEntity.ok(ApiResponse.exito(
                "Pestaña DOFA guardada.", semilleroRestMapper.toDetalleResponse(guardado)));
    }

    @PatchMapping("/{idSemillero}/pestana/ods")
    @Operation(summary = "Guardar pestaña ODS")
    public ResponseEntity<ApiResponse<SemilleroDetalleResponse>> guardarOds(
            @PathVariable Long idSemillero,
            @Valid @RequestBody GuardarPestanaOdsRequest request,
            @AuthenticationPrincipal CoordinadorPrincipal principal) {

        Semillero guardado = gestionarSemilleroUseCase.guardarPestanaOds(
                idSemillero, principal.getId(),
                request.getIdAreaOcde(),
                request.getSubAreaOcde(),
                request.getIdOdsPrincipal(),
                request.getObservacionesFinales()
        );

        return ResponseEntity.ok(ApiResponse.exito(
                "Pestaña ODS guardada.",
                semilleroRestMapper.toDetalleResponse(guardado)));
    }

    @GetMapping("/{idSemillero}/pestana/general")
    @Operation(summary = "Obtener datos de pestaña General")
    public ResponseEntity<ApiResponse<PestanaGeneralResponse>> obtenerGeneral(
            @PathVariable Long idSemillero,
            @AuthenticationPrincipal CoordinadorPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.exito(
                gestionarSemilleroUseCase.obtenerPestanaGeneral(
                        idSemillero, principal.getId())));
    }

    @GetMapping("/{idSemillero}/pestana/produccion")
    @Operation(summary = "Obtener datos de pestaña Producción")
    public ResponseEntity<ApiResponse<PestanaProduccionResponse>> obtenerProduccion(
            @PathVariable Long idSemillero,
            @AuthenticationPrincipal CoordinadorPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.exito(
                gestionarSemilleroUseCase.obtenerPestanaProduccion(
                        idSemillero, principal.getId())));
    }

    @GetMapping("/{idSemillero}/pestana/organizacion")
    @Operation(summary = "Obtener datos de pestaña Organización")
    public ResponseEntity<ApiResponse<PestanaOrganizacionResponse>> obtenerOrganizacion(
            @PathVariable Long idSemillero,
            @AuthenticationPrincipal CoordinadorPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.exito(
                gestionarSemilleroUseCase.obtenerPestanaOrganizacion(
                        idSemillero, principal.getId())));
    }

    @GetMapping("/{idSemillero}/pestana/relacionamiento")
    @Operation(summary = "Obtener datos de pestaña Relacionamiento")
    public ResponseEntity<ApiResponse<PestanaRelacionamientoResponse>> obtenerRelacionamiento(
            @PathVariable Long idSemillero,
            @AuthenticationPrincipal CoordinadorPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.exito(
                gestionarSemilleroUseCase.obtenerPestanaRelacionamiento(
                        idSemillero, principal.getId())));
    }

    @GetMapping("/{idSemillero}/pestana/actividades")
    @Operation(summary = "Obtener datos de pestaña Actividades")
    public ResponseEntity<ApiResponse<PestanaActividadesResponse>> obtenerActividades(
            @PathVariable Long idSemillero,
            @AuthenticationPrincipal CoordinadorPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.exito(
                gestionarSemilleroUseCase.obtenerPestanaActividades(
                        idSemillero, principal.getId())));
    }

    @GetMapping("/{idSemillero}/pestana/dofa")
    @Operation(summary = "Obtener datos de pestaña DOFA")
    public ResponseEntity<ApiResponse<PestanaDofaResponse>> obtenerDofa(
            @PathVariable Long idSemillero,
            @AuthenticationPrincipal CoordinadorPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.exito(
                gestionarSemilleroUseCase.obtenerPestanaDofa(
                        idSemillero, principal.getId())));
    }

    @GetMapping("/{idSemillero}/pestana/ods")
    @Operation(summary = "Obtener datos de pestaña ODS")
    public ResponseEntity<ApiResponse<PestanaOdsResponse>> obtenerOds(
            @PathVariable Long idSemillero,
            @AuthenticationPrincipal CoordinadorPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.exito(
                gestionarSemilleroUseCase.obtenerPestanaOds(
                        idSemillero, principal.getId())));
    }
}
