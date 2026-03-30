package co.udea.semilleros.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AreaOcde {
    private final Long id;
    private final String nombre;
    private final String descripcion;
}
