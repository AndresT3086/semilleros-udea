package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.model.asistencia.IntegranteAsistencia;
import co.udea.semilleros.domain.model.asistencia.Sesion;
import co.udea.semilleros.domain.model.asistencia.SesionDetalle;
import co.udea.semilleros.domain.port.in.GestionarAsistenciaUseCase;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.request.SesionRequest;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import co.udea.semilleros.infrastructure.security.filter.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coordinador/semilleros")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COORDINADOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Coordinador - Asistencia",
     description = "Registro de actividades de cada semillero y asistencia de sus integrantes")
public class AsistenciaController {

    private static final String PERIODO = "Año (2025) o semestre (2025-1, 2025-2). Vacío = todas las fechas";

    private final GestionarAsistenciaUseCase gestionarAsistenciaUseCase;

    @GetMapping("/{idSemillero}/sesiones")
    @Operation(summary = "Listar actividades del semillero", description = "Sesiones registradas con el conteo "
            + "de presentes, ausentes y excusados y el % de asistencia de cada una.")
    public ResponseEntity<ApiResponse<List<Sesion>>> listarSesiones(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long idSemillero,
            @Parameter(description = PERIODO) @RequestParam(required = false) String periodo
    ) {
        return ResponseEntity.ok(ApiResponse.exito(
                gestionarAsistenciaUseCase.listarSesiones(principal.getId(), idSemillero, periodo)));
    }

    @PostMapping("/{idSemillero}/sesiones")
    @Operation(summary = "Registrar actividad y asistencia", description = "Crea la sesión con la lista de "
            + "asistencia. Los integrantes activos que no se envíen quedan como AUSENTE.")
    public ResponseEntity<ApiResponse<SesionDetalle>> registrarSesion(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long idSemillero,
            @Valid @RequestBody SesionRequest request
    ) {
        SesionDetalle sesion = gestionarAsistenciaUseCase.registrarSesion(principal.getId(), idSemillero, request.toDatos());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.exito("Actividad registrada.", sesion));
    }

    @GetMapping("/sesiones/{idSesion}")
    @Operation(summary = "Detalle de una actividad con su lista de asistencia")
    public ResponseEntity<ApiResponse<SesionDetalle>> obtenerSesion(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long idSesion
    ) {
        return ResponseEntity.ok(ApiResponse.exito(gestionarAsistenciaUseCase.obtenerSesion(principal.getId(), idSesion)));
    }

    @PutMapping("/sesiones/{idSesion}")
    @Operation(summary = "Corregir actividad y asistencia", description = "Reemplaza los datos y la lista de asistencia.")
    public ResponseEntity<ApiResponse<SesionDetalle>> actualizarSesion(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long idSesion,
            @Valid @RequestBody SesionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.exito("Actividad actualizada.",
                gestionarAsistenciaUseCase.actualizarSesion(principal.getId(), idSesion, request.toDatos())));
    }

    @DeleteMapping("/sesiones/{idSesion}")
    @Operation(summary = "Eliminar actividad", description = "Elimina la sesión y su lista de asistencia.")
    public ResponseEntity<ApiResponse<Void>> eliminarSesion(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long idSesion
    ) {
        gestionarAsistenciaUseCase.eliminarSesion(principal.getId(), idSesion);
        return ResponseEntity.ok(ApiResponse.exito("Actividad eliminada.", null));
    }

    @GetMapping("/{idSemillero}/asistencia/integrantes")
    @Operation(summary = "Asistencia por integrante", description = "Integrantes activos (y retirados con registros "
            + "en el período) con su conteo y % de asistencia; las ausencias excusadas se descuentan.")
    public ResponseEntity<ApiResponse<List<IntegranteAsistencia>>> asistenciaPorIntegrante(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long idSemillero,
            @Parameter(description = PERIODO) @RequestParam(required = false) String periodo
    ) {
        return ResponseEntity.ok(ApiResponse.exito(
                gestionarAsistenciaUseCase.asistenciaPorIntegrante(principal.getId(), idSemillero, periodo)));
    }
}
