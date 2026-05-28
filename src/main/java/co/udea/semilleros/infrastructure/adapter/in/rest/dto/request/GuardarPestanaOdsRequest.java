package co.udea.semilleros.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GuardarPestanaOdsRequest {

    @NotNull(message = "El área OCDE es obligatoria")
    private Long idAreaOcde;

    private String subAreaOcde;

    @NotNull(message = "El ODS principal es obligatorio")
    private Long idOdsPrincipal;

    private String observacionesFinales;
}
