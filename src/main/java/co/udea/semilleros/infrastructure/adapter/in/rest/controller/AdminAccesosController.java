package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.model.acceso.DatosInvitacion;
import co.udea.semilleros.domain.model.acceso.EstadoSolicitud;
import co.udea.semilleros.domain.model.acceso.InvitacionEnviada;
import co.udea.semilleros.domain.port.in.AdministrarAccesosUseCase;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.request.AccesosRequests.InvitacionRequest;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.request.AccesosRequests.RechazoRequest;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.SolicitudAccesoResponse;
import co.udea.semilleros.infrastructure.security.filter.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administración - Accesos de coordinadores", description = "Revisión de solicitudes e invitaciones")
public class AdminAccesosController {

    private final AdministrarAccesosUseCase administrarAccesosUseCase;

    @GetMapping("/solicitudes-acceso")
    @Operation(summary = "Listar solicitudes de acceso", description = "Por defecto las PENDIENTES: correo ya "
            + "confirmado y esperando revisión. Las no verificadas no llegan al administrador.")
    public ResponseEntity<ApiResponse<List<SolicitudAccesoResponse>>> listar(
            @RequestParam(defaultValue = "PENDIENTE") EstadoSolicitud estado) {
        return ResponseEntity.ok(ApiResponse.exito(administrarAccesosUseCase.listarSolicitudes(estado).stream()
                .map(SolicitudAccesoResponse::de).toList()));
    }

    @GetMapping("/solicitudes-acceso/resumen")
    @Operation(summary = "Cantidad de solicitudes pendientes de revisión")
    public ResponseEntity<ApiResponse<Map<String, Long>>> resumen() {
        return ResponseEntity.ok(ApiResponse.exito(Map.of("pendientes", administrarAccesosUseCase.contarPendientes())));
    }

    @PostMapping("/solicitudes-acceso/{idSolicitud}/aprobar")
    @Operation(summary = "Aprobar solicitud", description = "Crea la cuenta de coordinador y envía el enlace para "
            + "crear la contraseña (vence en 24 horas).")
    public ResponseEntity<ApiResponse<Void>> aprobar(@PathVariable Long idSolicitud,
                                                     @AuthenticationPrincipal UsuarioPrincipal principal) {
        administrarAccesosUseCase.aprobar(idSolicitud, principal.getId());
        return ResponseEntity.ok(ApiResponse.exito("Solicitud aprobada. Se envió el enlace de activación.", null));
    }

    @PostMapping("/solicitudes-acceso/{idSolicitud}/rechazar")
    @Operation(summary = "Rechazar solicitud", description = "Notifica el motivo. La persona podrá volver a "
            + "solicitar después de 30 días, salvo que se bloquee el correo.")
    public ResponseEntity<ApiResponse<Void>> rechazar(@PathVariable Long idSolicitud,
                                                      @Valid @RequestBody RechazoRequest request,
                                                      @AuthenticationPrincipal UsuarioPrincipal principal) {
        administrarAccesosUseCase.rechazar(idSolicitud, principal.getId(), request.motivo(), request.bloquear());
        return ResponseEntity.ok(ApiResponse.exito("Solicitud rechazada.", null));
    }

    @PostMapping("/invitaciones")
    @Operation(summary = "Invitar a un coordinador", description = "Solo correos @udea.edu.co. Si la cuenta existe "
            + "pero no se ha activado, reenvía el enlace e invalida el anterior.")
    public ResponseEntity<ApiResponse<InvitacionEnviada>> invitar(@Valid @RequestBody InvitacionRequest request,
                                                                  @AuthenticationPrincipal UsuarioPrincipal principal) {
        InvitacionEnviada invitacion = administrarAccesosUseCase.invitar(
                new DatosInvitacion(request.nombres(), request.apellidos(), request.correo()), principal.getId());
        String mensaje = invitacion.reenviada() ? "Invitación reenviada." : "Invitación enviada.";
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.exito(mensaje, invitacion));
    }
}
