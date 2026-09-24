package co.udea.semilleros.domain.model.acceso;

import java.time.Instant;

/**
 * Enlace de un solo uso para que un coordinador aprobado o invitado cree su contraseña.
 */
public record TokenCuenta(Long id, Long idUsuario, OrigenToken origen, Instant expira, boolean usado) {

    public enum OrigenToken { APROBACION, INVITACION }
}
