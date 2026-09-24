package co.udea.semilleros.domain.port.out;

import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.model.Semillero;

public interface NotificacionEmailPort {

    void notificarNuevaInscripcion(Inscripcion inscripcion, String correoCoordinador);

    void notificarFinalizacionCaracterizacion(Semillero semillero, String correoAdministrador);

    /** Enlace para confirmar el correo de una solicitud de acceso como coordinador. */
    void enviarVerificacionSolicitud(String correo, String nombre, String token);

    /** Enlace para crear la contraseña tras una aprobación o una invitación. */
    void enviarActivacionCuenta(String correo, String nombre, String token, boolean invitacion);

    void notificarRechazoSolicitud(String correo, String nombre, String motivo);

    void enviarResumenSolicitudesPendientes(String correoAdministrador, long pendientes);
}
