package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean exitoso;
    private final String mensaje;
    private final T datos;
    private final String codigoError;

    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> exito(T datos) {
        return ApiResponse.<T>builder()
                .exitoso(true)
                .datos(datos)
                .build();
    }

    public static <T> ApiResponse<T> exito(String mensaje, T datos) {
        return ApiResponse.<T>builder()
                .exitoso(true)
                .mensaje(mensaje)
                .datos(datos)
                .build();
    }

    public static <T> ApiResponse<T> error(String codigoError, String mensaje) {
        return ApiResponse.<T>builder()
                .exitoso(false)
                .codigoError(codigoError)
                .mensaje(mensaje)
                .build();
    }
}
