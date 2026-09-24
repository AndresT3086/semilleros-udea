package co.udea.semilleros.domain.port.in;

import co.udea.semilleros.domain.model.acceso.DatosSolicitud;

/**
 * Pasos públicos del registro de coordinadores.
 */
public interface RegistroCoordinadorUseCase {

    /**
     * Registra la solicitud y envía el enlace de verificación. Si la solicitud no procede
     * (correo con cuenta, rechazo reciente, reenvío muy seguido...) no hace nada y no lo
     * informa, para no revelar qué correos existen.
     */
    void solicitarAcceso(DatosSolicitud datos);

    void verificarCorreo(String token);

    void activarCuenta(String token, String contrasena);
}
