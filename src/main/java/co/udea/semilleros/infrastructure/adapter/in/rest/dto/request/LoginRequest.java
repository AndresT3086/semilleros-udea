package co.udea.semilleros.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotNull(message = "La respuesta matemática es obligatoria")
    private Integer respuestaMath;

    @NotNull(message = "El operando1 es obligatorio")
    private Integer operando1;

    @NotNull(message = "El operando2 es obligatorio")
    private Integer operando2;
}
