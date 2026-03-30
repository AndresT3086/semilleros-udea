package co.udea.semilleros.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDateTime;

@Getter
@Builder
@With
public class Coordinador {

    private final Long id;
    private final String nombres;
    private final String apellidos;
    private final String correo;
    private final String passwordHash;
    private final String telefono;
    private final String rol;
    private final Boolean activo;
    private final LocalDateTime fechaCreacion;
}
