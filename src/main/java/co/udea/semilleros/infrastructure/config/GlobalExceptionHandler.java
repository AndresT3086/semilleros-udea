package co.udea.semilleros.infrastructure.config;

import co.udea.semilleros.domain.exception.AccesoNoAutorizadoException;
import co.udea.semilleros.domain.exception.CamposObligatoriosPendientesException;
import co.udea.semilleros.domain.exception.CredencialesInvalidasException;
import co.udea.semilleros.domain.exception.DatosAsistenciaInvalidosException;
import co.udea.semilleros.domain.exception.DominioCorreoNoPermitidoException;
import co.udea.semilleros.domain.exception.FiltroReporteInvalidoException;
import co.udea.semilleros.domain.exception.InscripcionDuplicadaException;
import co.udea.semilleros.domain.exception.RecursoNoEncontradoException;
import co.udea.semilleros.domain.exception.SemilleroYaExisteException;
import co.udea.semilleros.domain.exception.TokenInvalidoException;
import co.udea.semilleros.domain.exception.ValidacionBotException;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(DominioCorreoNoPermitidoException.class)
    public ResponseEntity<ApiResponse<Void>> handleDominioNoPermitido(DominioCorreoNoPermitidoException ex) {
        log.warn("Dominio de correo no permitido: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiResponse<Void>> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        log.warn("Intento de autenticación fallido");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(ValidacionBotException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidacionBot(ValidacionBotException ex) {
        log.warn("Validación anti-bot fallida");
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(AccesoNoAutorizadoException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccesoNoAutorizado(AccesoNoAutorizadoException ex) {
        log.warn("Acceso no autorizado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    // Sin este manejador, @PreAuthorize terminaría en el handler genérico (500) en lugar de 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccesoDenegado(AccessDeniedException ex) {
        log.warn("Acceso denegado por rol: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("ACCESO_NO_AUTORIZADO", "No tiene permisos para realizar esta operación."));
    }

    @ExceptionHandler(InscripcionDuplicadaException.class)
    public ResponseEntity<ApiResponse<Void>> handleInscripcionDuplicada(InscripcionDuplicadaException ex) {
        log.warn("Inscripción duplicada: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(SemilleroYaExisteException.class)
    public ResponseEntity<ApiResponse<Void>> handleSemilleroYaExiste(SemilleroYaExisteException ex) {
        log.warn("Semillero duplicado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(CamposObligatoriosPendientesException.class)
    public ResponseEntity<ApiResponse<Void>> handleCamposPendientes(CamposObligatoriosPendientesException ex) {
        log.warn("Campos obligatorios pendientes: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(DatosAsistenciaInvalidosException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatosAsistenciaInvalidos(DatosAsistenciaInvalidosException ex) {
        log.warn("Datos de asistencia inválidos: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(FiltroReporteInvalidoException.class)
    public ResponseEntity<ApiResponse<Void>> handleFiltroReporteInvalido(FiltroReporteInvalidoException ex) {
        log.warn("Filtro de reporte inválido: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenInvalido(TokenInvalidoException ex) {
        log.warn("Token inválido: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            String mensaje = error.getDefaultMessage();
            errores.put(campo, mensaje);
        });
        log.warn("Errores de validación en request: {}", errores);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .exitoso(false)
                        .codigoError("VALIDACION_FALLIDA")
                        .mensaje("Se encontraron errores de validación en la solicitud.")
                        .datos(errores)
                        .build());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleRutaNoEncontrada(Exception ex) {
        log.warn("Ruta no encontrada: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("RUTA_NO_ENCONTRADA", "El recurso solicitado no existe."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Error interno no controlado: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("ERROR_INTERNO", "Ocurrió un error interno. Por favor intente de nuevo."));
    }
}
