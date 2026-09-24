package co.udea.semilleros.domain.model.acceso;

import java.time.Instant;

/**
 * Resultado de una invitación: la cuenta queda inactiva hasta que la persona cree su contraseña.
 */
public record InvitacionEnviada(Long idUsuario, String correo, Instant expira, boolean reenviada) {
}
