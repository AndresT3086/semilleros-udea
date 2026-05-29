package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PestanaProduccionResponse {

    private Boolean tienenArticulos;
    private Integer cantidadArticulos;
    private Boolean tienenLibros;
    private Integer cantidadLibros;
    private Boolean organizanEventos;
    private Integer cantidadEventos;
    private Boolean participaEnEventos;
    private Integer cantidadParticipaciones;
}
