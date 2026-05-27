package co.udea.semilleros.domain.port.in;

import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.model.Semillero;

import java.util.List;

public interface GestionarSemilleroUseCase {

    Semillero crearSemilleroBorrador(Long idCoordinador);

    Semillero guardarPestanaGeneral(Long idSemillero, Long idCoordinador, Semillero datos);

    List<Semillero> obtenerSemillerosDelCoordinador(Long idCoordinador);

    Semillero obtenerSemilleroDelCoordinadorPorId(Long idSemillero, Long idCoordinador);

    Semillero finalizarCaracterizacion(Long idSemillero, Long idCoordinador);

    List<Inscripcion> listarInscripcionesPendientes(Long idSemillero, Long idCoordinador);

    Inscripcion aprobarInscripcion(Long idInscripcion, Long idCoordinador);

    Inscripcion rechazarInscripcion(Long idInscripcion, Long idCoordinador);
}
