package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FiltroItemResponse {
    private Long id;
    private String nombre;
    private String siglas;
}
