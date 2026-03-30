package co.udea.semilleros.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GuardarPestanaGeneralRequest {

    @NotBlank(message = "El nombre del semillero es obligatorio")
    @Size(max = 300, message = "El nombre no puede superar los 300 caracteres")
    private String nombre;

    @Size(max = 30, message = "Las siglas no pueden superar los 30 caracteres")
    private String siglas;

    @NotBlank(message = "El correo del semillero es obligatorio")
    @Email(message = "El correo del semillero no tiene un formato válido")
    private String correoSemillero;

    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    private String telefono;

    @Min(value = 1900, message = "El año de creación debe ser mayor a 1900")
    @Max(value = 2100, message = "El año de creación no es válido")
    private Integer anioCreacion;

    @NotBlank(message = "La misión es obligatoria")
    private String mision;

    @NotBlank(message = "La visión es obligatoria")
    private String vision;

    @NotBlank(message = "El objetivo es obligatorio")
    private String objetivo;

    private String lineasInvestigacion;

    @Size(max = 500, message = "Las palabras clave no pueden superar los 500 caracteres")
    private String palabrasClave;

    @Size(max = 200, message = "El grupo de investigación no puede superar los 200 caracteres")
    private String grupoInvestigacion;

    @NotNull(message = "La unidad académica es obligatoria")
    private Long idUnidadAcademica;

    @NotNull(message = "El campus es obligatorio")
    private Long idCampus;

    @NotNull(message = "El área OCDE es obligatoria")
    private Long idAreaOcde;
}
