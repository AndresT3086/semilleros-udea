package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.AccesoNoAutorizadoException;
import co.udea.semilleros.domain.exception.DatosAsistenciaInvalidosException;
import co.udea.semilleros.domain.exception.RecursoNoEncontradoException;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.model.asistencia.DatosSesion;
import co.udea.semilleros.domain.model.asistencia.EstadoAsistencia;
import co.udea.semilleros.domain.model.asistencia.IntegranteAsistencia;
import co.udea.semilleros.domain.model.asistencia.Sesion;
import co.udea.semilleros.domain.model.asistencia.SesionDetalle;
import co.udea.semilleros.domain.model.reporte.ReporteFiltro;
import co.udea.semilleros.domain.port.in.GestionarAsistenciaUseCase;
import co.udea.semilleros.domain.port.out.AsistenciaRepositoryPort;
import co.udea.semilleros.domain.port.out.SemilleroRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GestionarAsistenciaUseCaseImpl implements GestionarAsistenciaUseCase {

    private static final String SESION = "Sesión";

    private final AsistenciaRepositoryPort asistenciaRepositoryPort;
    private final SemilleroRepositoryPort semilleroRepositoryPort;
    private final Clock clock;

    @Override
    public List<Sesion> listarSesiones(Long idCoordinador, Long idSemillero, String periodo) {
        validarPropiedad(idCoordinador, idSemillero);
        ReporteFiltro rango = rango(periodo);
        return asistenciaRepositoryPort.listarSesiones(idSemillero, rango.inicioPeriodo(), rango.fechaCorte());
    }

    @Override
    public SesionDetalle obtenerSesion(Long idCoordinador, Long idSesion) {
        validarPropiedadDeSesion(idCoordinador, idSesion);
        return detalle(idSesion);
    }

    /**
     * La lista se toma de los integrantes activos al registrar: quienes no vengan en la
     * solicitud quedan como AUSENTE, así ningún integrante activo se omite del cálculo.
     */
    @Override
    @Transactional
    public SesionDetalle registrarSesion(Long idCoordinador, Long idSemillero, DatosSesion datos) {
        validarPropiedad(idCoordinador, idSemillero);
        List<Long> activos = asistenciaRepositoryPort.idsIntegrantesActivos(idSemillero);
        validarDatos(datos, new HashSet<>(activos));
        Long idSesion = asistenciaRepositoryPort.crearSesion(idSemillero, completarLista(datos, activos));
        return detalle(idSesion);
    }

    /**
     * La corrección reemplaza la lista. Se aceptan quienes ya estaban en la sesión y los
     * integrantes activos (por ejemplo, alguien vinculado después del registro inicial).
     */
    @Override
    @Transactional
    public SesionDetalle actualizarSesion(Long idCoordinador, Long idSesion, DatosSesion datos) {
        Long idSemillero = validarPropiedadDeSesion(idCoordinador, idSesion);
        Set<Long> permitidos = new HashSet<>(asistenciaRepositoryPort.idsIntegrantesDeSesion(idSesion));
        permitidos.addAll(asistenciaRepositoryPort.idsIntegrantesActivos(idSemillero));
        validarDatos(datos, permitidos);
        asistenciaRepositoryPort.actualizarSesion(idSesion, datos);
        return detalle(idSesion);
    }

    @Override
    @Transactional
    public void eliminarSesion(Long idCoordinador, Long idSesion) {
        validarPropiedadDeSesion(idCoordinador, idSesion);
        asistenciaRepositoryPort.eliminarSesion(idSesion);
    }

    @Override
    public List<IntegranteAsistencia> asistenciaPorIntegrante(Long idCoordinador, Long idSemillero, String periodo) {
        validarPropiedad(idCoordinador, idSemillero);
        ReporteFiltro rango = rango(periodo);
        return asistenciaRepositoryPort.asistenciaPorIntegrante(idSemillero, rango.inicioPeriodo(), rango.fechaCorte());
    }

    private static ReporteFiltro rango(String periodo) {
        return ReporteFiltro.de(periodo, null, null, null, null);
    }

    private SesionDetalle detalle(Long idSesion) {
        return asistenciaRepositoryPort.obtenerSesion(idSesion)
                .orElseThrow(() -> new RecursoNoEncontradoException(SESION, idSesion));
    }

    private void validarPropiedad(Long idCoordinador, Long idSemillero) {
        Semillero semillero = semilleroRepositoryPort.buscarPorId(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", idSemillero));
        if (!Objects.equals(semillero.getIdCoordinador(), idCoordinador)) {
            throw new AccesoNoAutorizadoException("asistencia del semillero " + idSemillero);
        }
    }

    private Long validarPropiedadDeSesion(Long idCoordinador, Long idSesion) {
        Long idSemillero = asistenciaRepositoryPort.semilleroDeSesion(idSesion)
                .orElseThrow(() -> new RecursoNoEncontradoException(SESION, idSesion));
        validarPropiedad(idCoordinador, idSemillero);
        return idSemillero;
    }

    private void validarDatos(DatosSesion datos, Set<Long> integrantesPermitidos) {
        if (datos.fecha().isAfter(LocalDate.now(clock))) {
            throw new DatosAsistenciaInvalidosException("No se puede registrar asistencia de una fecha futura.");
        }
        if (datos.idActividad() != null && !asistenciaRepositoryPort.existeActividad(datos.idActividad())) {
            throw new DatosAsistenciaInvalidosException("El tipo de actividad " + datos.idActividad() + " no existe.");
        }
        Set<Long> vistos = new HashSet<>();
        for (DatosSesion.Registro registro : datos.asistencias()) {
            if (!vistos.add(registro.idIntegrante())) {
                throw new DatosAsistenciaInvalidosException(
                        "El integrante " + registro.idIntegrante() + " aparece más de una vez en la lista.");
            }
            if (!integrantesPermitidos.contains(registro.idIntegrante())) {
                throw new DatosAsistenciaInvalidosException(
                        "El integrante " + registro.idIntegrante() + " no es un integrante activo de este semillero.");
            }
        }
    }

    private static DatosSesion completarLista(DatosSesion datos, List<Long> activos) {
        Set<Long> incluidos = new LinkedHashSet<>();
        List<DatosSesion.Registro> lista = new ArrayList<>(datos.asistencias());
        datos.asistencias().forEach(registro -> incluidos.add(registro.idIntegrante()));
        activos.stream()
                .filter(id -> !incluidos.contains(id))
                .forEach(id -> lista.add(new DatosSesion.Registro(id, EstadoAsistencia.AUSENTE)));
        return new DatosSesion(datos.titulo(), datos.fecha(), datos.idActividad(), lista);
    }
}
