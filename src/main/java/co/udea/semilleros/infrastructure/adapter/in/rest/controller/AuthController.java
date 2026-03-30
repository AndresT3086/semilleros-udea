package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.port.in.AutenticarCoordinadorUseCase;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.CaptchaMathResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.LoginResponse;
import co.udea.semilleros.infrastructure.security.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de autenticación para coordinadores de semilleros")
public class AuthController {

    private final AutenticarCoordinadorUseCase autenticarCoordinadorUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/captcha-math")
    @Operation(
        summary = "Obtener desafío matemático anti-bot",
        description = "Genera una operación matemática simple que el usuario debe resolver antes de hacer login. "
                    + "Envíe los operandos y la respuesta en el cuerpo del login."
    )
    public ResponseEntity<ApiResponse<CaptchaMathResponse>> obtenerCaptchaMath() {
        int op1 = ThreadLocalRandom.current().nextInt(1, 20);
        int op2 = ThreadLocalRandom.current().nextInt(1, 20);

        CaptchaMathResponse captcha = CaptchaMathResponse.builder()
                .operando1(op1)
                .operando2(op2)
                .operacion("SUMA")
                .pregunta(String.format("¿Cuánto es %d + %d?", op1, op2))
                .build();

        return ResponseEntity.ok(ApiResponse.exito(captcha));
    }

    @PostMapping("/login")
    @Operation(
        summary = "Iniciar sesión como coordinador",
        description = "Autentica a un coordinador con correo @udea.edu.co, contraseña y validación matemática anti-bot. "
                    + "Retorna un token JWT válido para usar en los endpoints protegidos."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Autenticación exitosa",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Credenciales inválidas o dominio no permitido"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validación matemática incorrecta")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        String token = autenticarCoordinadorUseCase.autenticar(
                request.getCorreo(),
                request.getPassword(),
                request.getRespuestaMath(),
                request.getOperando1(),
                request.getOperando2()
        );

        Long idCoordinador = jwtTokenProvider.extraerIdCoordinador(token);

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .tipo("Bearer")
                .correo(request.getCorreo())
                .idCoordinador(idCoordinador)
                .build();

        return ResponseEntity.ok(ApiResponse.exito("Autenticación exitosa.", response));
    }
}
