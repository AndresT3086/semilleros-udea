package co.udea.semilleros.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GuardarPestanaDofaRequest {

    @NotBlank(message = "Las fortalezas son obligatorias")
    private String fortalezas;

    @NotBlank(message = "Las debilidades son obligatorias")
    private String debilidades;

    @NotBlank(message = "Las oportunidades son obligatorias")
    private String oportunidades;

    @NotBlank(message = "Las amenazas son obligatorias")
    private String amenazas;
}
