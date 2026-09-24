package co.udea.semilleros.domain.port.in;

import co.udea.semilleros.domain.model.asistencia.DatosSesion;
import co.udea.semilleros.domain.model.asistencia.IntegranteAsistencia;
import co.udea.semilleros.domain.model.asistencia.Sesion;
import co.udea.semilleros.domain.model.asistencia.SesionDetalle;

import java.util.List;

/**
 * Registro de actividades y asistencia por parte del coordinador de cada semillero.
 * {@code periodo} usa el formato de reportes: "2025", "2025-1", "2025-2" o null (todo).
 */
public interface GestionarAsistenciaUseCase {

    List<Sesion> listarSesiones(Long idCoordinador, Long idSemillero, String periodo);

    SesionDetalle obtenerSesion(Long idCoordinador, Long idSesion);

    SesionDetalle registrarSesion(Long idCoordinador, Long idSemillero, DatosSesion datos);

    SesionDetalle actualizarSesion(Long idCoordinador, Long idSesion, DatosSesion datos);

    void eliminarSesion(Long idCoordinador, Long idSesion);

    List<IntegranteAsistencia> asistenciaPorIntegrante(Long idCoordinador, Long idSemillero, String periodo);
}
