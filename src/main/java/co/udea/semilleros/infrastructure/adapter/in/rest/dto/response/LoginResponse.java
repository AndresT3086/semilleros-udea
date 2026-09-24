package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String token;
    private String tipo;
    private String correo;
    private Long idUsuario;
}
