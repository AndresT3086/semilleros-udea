package co.udea.semilleros.domain.model.acceso;

/**
 * Invitación directa de un administrador a un coordinador (solo correos institucionales).
 */
public record DatosInvitacion(String nombres, String apellidos, String correo) {
}
