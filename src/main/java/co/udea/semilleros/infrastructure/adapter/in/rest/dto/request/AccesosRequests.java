package co.udea.semilleros.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpos de las peticiones de verificación, activación, rechazo e invitación.
 */
public final class AccesosRequests {

    private AccesosRequests() {
    }

    public record TokenRequest(@NotBlank(message = "El enlace no es válido") String token) {
    }

    public record ActivarCuentaRequest(
            @NotBlank(message = "El enlace no es válido") String token,
            @NotBlank(message = "La contraseña es obligatoria") String contrasena
    ) {
        @Override
        public String toString() {
            return "ActivarCuentaRequest[token=***, contrasena=***]";
        }
    }

    public record RechazoRequest(
            @NotBlank(message = "Indica el motivo del rechazo")
            @Size(max = 500, message = "El motivo no puede superar los 500 caracteres") String motivo,
            boolean bloquear
    ) {
    }

    public record InvitacionRequest(
            @NotBlank(message = "Los nombres son obligatorios") @Size(max = 100) String nombres,
            @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo debe tener un formato válido")
            @Size(max = 150) String correo
    ) {
    }
}
