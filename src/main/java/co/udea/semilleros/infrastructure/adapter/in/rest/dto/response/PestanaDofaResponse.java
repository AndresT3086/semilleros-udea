package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PestanaDofaResponse {
    private String fortalezas;
    private String debilidades;
    private String oportunidades;
    private String amenazas;
}
