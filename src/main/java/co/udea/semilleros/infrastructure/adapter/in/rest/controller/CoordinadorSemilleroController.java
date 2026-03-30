package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.port.in.GestionarSemilleroUseCase;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.request.GuardarPestanaGeneralRequest;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.SemilleroDetalleResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                    + "Solo puede tener un semillero activo por coordinador."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Semillero borrador creado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "El coordinador ya tiene un semillero activo"),
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

    @GetMapping("/mi-semillero")
    @Operation(
        summary = "Obtener semillero del coordinador autenticado",
        description = "Retorna el semillero asociado al coordinador autenticado con el progreso de caracterización."
    )
    public ResponseEntity<ApiResponse<SemilleroDetalleResponse>> obtenerMiSemillero(
            @AuthenticationPrincipal CoordinadorPrincipal principal
    ) {
        Semillero semillero = gestionarSemilleroUseCase.obtenerSemilleroDelCoordinador(principal.getId());
        return ResponseEntity.ok(ApiResponse.exito(semilleroRestMapper.toDetalleResponse(semillero)));
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
}
