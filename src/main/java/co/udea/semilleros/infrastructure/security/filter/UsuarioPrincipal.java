package co.udea.semilleros.infrastructure.security.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UsuarioPrincipal {
    private final Long id;
    private final String correo;
    private final String rol;
}
