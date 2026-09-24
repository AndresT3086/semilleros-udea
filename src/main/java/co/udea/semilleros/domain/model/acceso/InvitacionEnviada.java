package co.udea.semilleros.domain.model.acceso;

import java.time.Instant;

/**
 * Resultado de una invitación: la cuenta queda inactiva hasta que la persona cree su contraseña.
 * {@code correoEnviado} es falso si el proveedor de correo rechazó el envío; invitar de nuevo reenvía el enlace.
 */
public record InvitacionEnviada(Long idUsuario, String correo, Instant expira, boolean reenviada, boolean correoEnviado) {
}
