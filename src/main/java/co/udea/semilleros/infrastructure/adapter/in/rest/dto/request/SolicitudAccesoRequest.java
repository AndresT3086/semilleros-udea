package co.udea.semilleros.infrastructure.adapter.in.rest.dto.request;

import co.udea.semilleros.domain.model.acceso.DatosSolicitud;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SolicitudAccesoRequest {

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden superar los 100 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
    private String apellidos;

    @NotBlank(message = "La cédula es obligatoria")
    @Pattern(regexp = "^[0-9]{7,10}$", message = "La cédula debe tener entre 7 y 10 dígitos numéricos")
    private String cedula;

    @NotBlank(message = "El correo institucional es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
    private String correo;

    private Long idUnidadAcademica;

    @NotBlank(message = "Cuéntanos qué semillero coordinas o coordinarás")
    @Size(min = 20, max = 1000, message = "La justificación debe tener entre 20 y 1000 caracteres")
    private String justificacion;

    /** Campo trampa: el formulario lo oculta, así que una persona lo envía vacío. */
    private String sitioWeb;

    @NotNull(message = "La respuesta matemática es obligatoria")
    private Integer respuestaMath;

    @NotNull(message = "El operando 1 es obligatorio")
    private Integer operando1;

    @NotNull(message = "El operando 2 es obligatorio")
    private Integer operando2;

    public DatosSolicitud toDatos(String ip) {
        return new DatosSolicitud(nombres, apellidos, cedula, correo, idUnidadAcademica, justificacion, sitioWeb,
                respuestaMath, operando1, operando2, ip);
    }
}
