package co.udea.semilleros.domain.port.out;

import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.model.Semillero;

public interface NotificacionEmailPort {

    void notificarNuevaInscripcion(Inscripcion inscripcion, String correoCoordinador);

    void notificarFinalizacionCaracterizacion(Semillero semillero, String correoAdministrador);

}
