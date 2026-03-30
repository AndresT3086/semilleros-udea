package co.udea.semilleros.domain.port.in;

import co.udea.semilleros.domain.model.Semillero;

public interface GestionarSemilleroUseCase {

    Semillero crearSemilleroBorrador(Long idCoordinador);

    Semillero guardarPestanaGeneral(Long idSemillero, Long idCoordinador, Semillero datos);

    Semillero obtenerSemilleroDelCoordinador(Long idCoordinador);

    Semillero finalizarCaracterizacion(Long idSemillero, Long idCoordinador);
}
