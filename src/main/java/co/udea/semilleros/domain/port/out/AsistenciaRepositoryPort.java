package co.udea.semilleros.domain.port.out;

import co.udea.semilleros.domain.model.asistencia.DatosSesion;
import co.udea.semilleros.domain.model.asistencia.IntegranteAsistencia;
import co.udea.semilleros.domain.model.asistencia.Sesion;
import co.udea.semilleros.domain.model.asistencia.SesionDetalle;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia de sesiones de semillero y asistencia de sus integrantes.
 * Los rangos de fecha son inclusivos; un extremo null no limita la consulta.
 */
public interface AsistenciaRepositoryPort {

    /** Ids de los integrantes activos del semillero. */
    List<Long> idsIntegrantesActivos(Long idSemillero);

    /** Ids de los integrantes que están en la lista de la sesión. */
    List<Long> idsIntegrantesDeSesion(Long idSesion);

    boolean existeActividad(Long idActividad);

    Long crearSesion(Long idSemillero, DatosSesion datos);

    void actualizarSesion(Long idSesion, DatosSesion datos);

    void eliminarSesion(Long idSesion);

    Optional<Long> semilleroDeSesion(Long idSesion);

    Optional<SesionDetalle> obtenerSesion(Long idSesion);

    List<Sesion> listarSesiones(Long idSemillero, LocalDate desde, LocalDate hasta);

    /** Integrantes activos y los que tengan asistencia registrada en el rango, con su conteo. */
    List<IntegranteAsistencia> asistenciaPorIntegrante(Long idSemillero, LocalDate desde, LocalDate hasta);
}
