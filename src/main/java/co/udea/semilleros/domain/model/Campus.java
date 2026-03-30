package co.udea.semilleros.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Campus {
    private final Long id;
    private final String nombre;
    private final String ciudad;
    private final String departamento;
    private final String direccion;
}
