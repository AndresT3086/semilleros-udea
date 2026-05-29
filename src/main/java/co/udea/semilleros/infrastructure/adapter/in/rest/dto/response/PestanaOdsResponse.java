package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PestanaOdsResponse {

    private Long   idAreaOcde;
    private String nombreAreaOcde;
    private String subAreaOcde;
    private Long   idOdsPrincipal;
    private String nombreOdsPrincipal;
    private String observacionesFinales;
}
