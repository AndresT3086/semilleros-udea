package co.udea.semilleros.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InscripcionRequest {

    @NotNull(message = "El id del semillero es obligatorio")
    private Long idSemillero;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
    private String apellidos;

    @NotBlank(message = "La cédula es obligatoria")
    @Pattern(regexp = "^[0-9]{7,10}$", message = "La cédula debe tener entre 7 y 10 dígitos numéricos")
    private String cedula;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido (ejemplo@dominio.com)")
    private String correo;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe tener exactamente 10 dígitos numéricos")
    private String telefono;

    @Pattern(regexp = "^(FEMENINO|MASCULINO|OTRO)$", message = "El sexo debe ser FEMENINO, MASCULINO u OTRO")
    private String sexo;

    @Size(max = 200, message = "El programa no puede superar los 200 caracteres")
    private String programa;

    @Size(max = 10, message = "El semestre no puede superar los 10 caracteres")
    private String semestre;

    @Size(max = 2000, message = "La motivación no puede superar los 2000 caracteres")
    private String motivacion;

    @NotNull(message = "Debe aceptar los términos y condiciones")
    @AssertTrue(message = "Debe aceptar los términos y condiciones para continuar")
    private Boolean aceptaTerminos;
}
