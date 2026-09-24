package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.port.in.RegistroCoordinadorUseCase;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.request.AccesosRequests.ActivarCuentaRequest;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.request.AccesosRequests.TokenRequest;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.request.SolicitudAccesoRequest;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro público de coordinadores: solicitud, confirmación del correo y activación de la cuenta.
 * Límites por IP en {@code RateLimitFilter}.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Registro de coordinadores", description = "Solicitud de acceso, confirmación de correo y activación de cuenta")
public class RegistroCoordinadorController {

    static final String MENSAJE_SOLICITUD = "Si los datos son válidos, recibirás en tu correo institucional un enlace "
            + "para confirmar la solicitud. Revisa también la carpeta de spam.";

    private final RegistroCoordinadorUseCase registroCoordinadorUseCase;

    @PostMapping("/solicitudes-acceso")
    @Operation(summary = "Solicitar acceso como coordinador", description = "Solo correos @udea.edu.co. La respuesta "
            + "es la misma aunque la solicitud no proceda, para no revelar qué correos existen.")
    public ResponseEntity<ApiResponse<Void>> solicitarAcceso(@Valid @RequestBody SolicitudAccesoRequest request,
                                                             HttpServletRequest http) {
        registroCoordinadorUseCase.solicitarAcceso(request.toDatos(ipOrigen(http)));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.exito(MENSAJE_SOLICITUD, null));
    }

    @PostMapping("/solicitudes-acceso/verificar")
    @Operation(summary = "Confirmar el correo de una solicitud", description = "Con el token del enlace enviado por correo.")
    public ResponseEntity<ApiResponse<Void>> verificarCorreo(@Valid @RequestBody TokenRequest request) {
        registroCoordinadorUseCase.verificarCorreo(request.token());
        return ResponseEntity.ok(ApiResponse.exito("Correo confirmado. Un administrador revisará tu solicitud y "
                + "te avisaremos por correo.", null));
    }

    @PostMapping("/cuenta/activar")
    @Operation(summary = "Crear la contraseña y activar la cuenta", description = "Con el token del enlace de "
            + "aprobación o invitación. La contraseña debe tener 10 a 72 caracteres, con letras y números.")
    public ResponseEntity<ApiResponse<Void>> activarCuenta(@Valid @RequestBody ActivarCuentaRequest request) {
        registroCoordinadorUseCase.activarCuenta(request.token(), request.contrasena());
        return ResponseEntity.ok(ApiResponse.exito("Cuenta activada. Ya puedes iniciar sesión.", null));
    }

    private static String ipOrigen(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : http.getRemoteAddr();
        return ip != null && ip.length() > 64 ? ip.substring(0, 64) : ip;
    }
}
