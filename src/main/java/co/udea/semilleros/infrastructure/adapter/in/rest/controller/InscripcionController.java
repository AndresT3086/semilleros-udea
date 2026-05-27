package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.port.in.InscribirseASemilleroUseCase;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.request.InscripcionRequest;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.InscripcionResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.mapper.SemilleroRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inscripciones")
@RequiredArgsConstructor
@Tag(name = "Inscripciones", description = "Endpoints para inscripción de estudiantes a semilleros. "
        + "Solo se permiten correos con dominio @udea.edu.co")
public class InscripcionController {

    private final InscribirseASemilleroUseCase inscribirseASemilleroUseCase;
    private final SemilleroRestMapper semilleroRestMapper;

    @PostMapping
    @Operation(
        summary = "Inscribirse a un semillero",
        description = "Permite a un estudiante   solicitar inscripción a un semillero activo. "
                    + "La solicitud queda en estado PENDIENTE y se notifica al coordinador."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "Inscripción registrada exitosamente",
            content = @Content(schema = @Schema(implementation = InscripcionResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos o correo no institucional"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Ya existe una inscripción activa para este correo y semillero"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Semillero no encontrado")
    })
    public ResponseEntity<ApiResponse<InscripcionResponse>> inscribirse(
            @Valid @RequestBody InscripcionRequest request
    ) {
        Inscripcion inscripcion = semilleroRestMapper.toInscripcionDomain(request);
        Inscripcion guardada = inscribirseASemilleroUseCase.inscribir(inscripcion);
        InscripcionResponse response = semilleroRestMapper.toInscripcionResponse(guardada);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.exito(
                        "Solicitud de inscripción registrada exitosamente. El coordinador será notificado.",
                        response));
    }
}
