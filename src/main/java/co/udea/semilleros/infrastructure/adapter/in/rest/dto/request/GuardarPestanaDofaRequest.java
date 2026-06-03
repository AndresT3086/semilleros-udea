package co.udea.semilleros.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GuardarPestanaDofaRequest {

    private static final String MENSAJE_DOFA_OBLIGATORIO =
            "Completa las cuatro áreas del análisis DOFA: fortalezas, debilidades, oportunidades y amenazas.";

    @NotBlank(message = MENSAJE_DOFA_OBLIGATORIO)
    private String fortalezas;

    @NotBlank(message = MENSAJE_DOFA_OBLIGATORIO)
    private String debilidades;

    @NotBlank(message = MENSAJE_DOFA_OBLIGATORIO)
    private String oportunidades;

    @NotBlank(message = MENSAJE_DOFA_OBLIGATORIO)
    private String amenazas;
}
