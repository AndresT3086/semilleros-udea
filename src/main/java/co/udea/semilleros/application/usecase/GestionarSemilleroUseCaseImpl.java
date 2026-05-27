package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.AccesoNoAutorizadoException;
import co.udea.semilleros.domain.exception.CamposObligatoriosPendientesException;
import co.udea.semilleros.domain.exception.RecursoNoEncontradoException;
import co.udea.semilleros.domain.exception.SemilleroYaExisteException;
import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.port.in.GestionarSemilleroUseCase;
import co.udea.semilleros.domain.port.out.InscripcionRepositoryPort;
import co.udea.semilleros.domain.port.out.NotificacionEmailPort;
import co.udea.semilleros.domain.port.out.SemilleroIntegranteRepositoryPort;
import co.udea.semilleros.domain.port.out.SemilleroRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GestionarSemilleroUseCaseImpl implements GestionarSemilleroUseCase {

    private final SemilleroRepositoryPort semilleroRepositoryPort;
    private final InscripcionRepositoryPort inscripcionRepositoryPort;
    private final SemilleroIntegranteRepositoryPort semilleroIntegranteRepositoryPort;
    private final NotificacionEmailPort notificacionEmailPort;

    @Value("${app.admin.correo:admin@udea.edu.co}")
    private String correoAdministrador;

    @Override
    @Transactional
    public Semillero crearSemilleroBorrador(Long idCoordinador) {

        String codigo = generarCodigoUnico();

        Semillero borrador = Semillero.builder()
                .codigo(codigo)
                .estado(Semillero.EstadoSemillero.BORRADOR)
                .estadoCaracterizacion("GENERAL_PENDIENTE")
                .idCoordinador(idCoordinador)
                .fechaCreacion(LocalDateTime.now())
                .build();

        return semilleroRepositoryPort.guardar(borrador);
    }

    @Override
    @Transactional
    public Semillero guardarPestanaGeneral(Long idSemillero, Long idCoordinador, Semillero datos) {
        Semillero existente = semilleroRepositoryPort.buscarPorId(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", idSemillero));

        validarPropiedadDelCoordinador(existente, idCoordinador);
        validarCamposObligatoriosGeneral(datos);

        if (!existente.getNombre().equals(datos.getNombre())
                && semilleroRepositoryPort.existePorNombre(datos.getNombre())) {
            throw new SemilleroYaExisteException("nombre", datos.getNombre());
        }

        Semillero actualizado = existente
                .withNombre(datos.getNombre())
                .withSiglas(datos.getSiglas())
                .withCorreoSemillero(datos.getCorreoSemillero())
                .withTelefono(datos.getTelefono())
                .withAnioCreacion(datos.getAnioCreacion())
                .withMision(datos.getMision())
                .withVision(datos.getVision())
                .withObjetivo(datos.getObjetivo())
                .withLineasInvestigacion(datos.getLineasInvestigacion())
                .withPalabrasClave(datos.getPalabrasClave())
                .withGrupoInvestigacion(datos.getGrupoInvestigacion())
                .withIdUnidadAcademica(datos.getIdUnidadAcademica())
                .withIdCampus(datos.getIdCampus())
                .withIdAreaOcde(datos.getIdAreaOcde())
                .withEstadoCaracterizacion("GENERAL_COMPLETADO")
                .withFechaActualizacion(LocalDateTime.now());

        return semilleroRepositoryPort.guardar(actualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Semillero> obtenerSemillerosDelCoordinador(Long idCoordinador) {
        return semilleroRepositoryPort.buscarPorCoordinador(idCoordinador);
    }

    @Override
    @Transactional(readOnly = true)
    public Semillero obtenerSemilleroDelCoordinadorPorId(Long idSemillero, Long idCoordinador) {
        Semillero semillero = semilleroRepositoryPort.buscarPorId(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", idSemillero));

        validarPropiedadDelCoordinador(semillero, idCoordinador);
        return semillero;
    }

    @Override
    @Transactional
    public Semillero finalizarCaracterizacion(Long idSemillero, Long idCoordinador) {
        Semillero semillero = semilleroRepositoryPort.buscarPorId(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", idSemillero));

        validarPropiedadDelCoordinador(semillero, idCoordinador);

        Semillero finalizado = semillero
                .withEstado(Semillero.EstadoSemillero.CARACTERIZADO)
                .withEstadoCaracterizacion("COMPLETO")
                .withFechaActualizacion(LocalDateTime.now());

        Semillero guardado = semilleroRepositoryPort.guardar(finalizado);

        notificacionEmailPort.notificarFinalizacionCaracterizacion(guardado, correoAdministrador);

        return guardado;
    }

    private void validarPropiedadDelCoordinador(Semillero semillero, Long idCoordinador) {
        if (!semillero.getIdCoordinador().equals(idCoordinador)) {
            throw new AccesoNoAutorizadoException("semillero con id " + semillero.getId());
        }
    }

    private void validarCamposObligatoriosGeneral(Semillero datos) {
        List<String> faltantes = new ArrayList<>();

        if (datos.getNombre() == null || datos.getNombre().isBlank()) faltantes.add("nombre");
        if (datos.getCorreoSemillero() == null || datos.getCorreoSemillero().isBlank()) faltantes.add("correoSemillero");
        if (datos.getMision() == null || datos.getMision().isBlank()) faltantes.add("mision");
        if (datos.getVision() == null || datos.getVision().isBlank()) faltantes.add("vision");
        if (datos.getObjetivo() == null || datos.getObjetivo().isBlank()) faltantes.add("objetivo");
        if (datos.getIdUnidadAcademica() == null) faltantes.add("unidadAcademica");
        if (datos.getIdCampus() == null) faltantes.add("campus");
        if (datos.getIdAreaOcde() == null) faltantes.add("areaOcde");

        if (!faltantes.isEmpty()) {
            throw new CamposObligatoriosPendientesException("General", faltantes);
        }
    }

    private String generarCodigoUnico() {
        long consecutivo = semilleroRepositoryPort.contarPorEstado(Semillero.EstadoSemillero.ACTIVO)
                + semilleroRepositoryPort.contarPorEstado(Semillero.EstadoSemillero.BORRADOR)
                + semilleroRepositoryPort.contarPorEstado(Semillero.EstadoSemillero.CARACTERIZADO)
                + 1;

        String candidato = String.format("SEM-UDEA-%04d", consecutivo);

        while (semilleroRepositoryPort.existePorCodigo(candidato)) {
            consecutivo++;
            candidato = String.format("SEM-UDEA-%04d", consecutivo);
        }

        return candidato;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inscripcion> listarInscripcionesPendientes(Long idSemillero, Long idCoordinador) {
        Semillero semillero = semilleroRepositoryPort.buscarPorId(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", idSemillero));

        validarPropiedadDelCoordinador(semillero, idCoordinador);

        return inscripcionRepositoryPort.buscarPorSemilleroYEstado(
                idSemillero, Inscripcion.EstadoInscripcion.PENDIENTE);
    }

    @Override
    @Transactional
    public Inscripcion aprobarInscripcion(Long idInscripcion, Long idCoordinador) {
        Inscripcion inscripcion = inscripcionRepositoryPort.buscarPorId(idInscripcion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción", idInscripcion));

        Semillero semillero = semilleroRepositoryPort.buscarPorId(inscripcion.getIdSemillero())
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", inscripcion.getIdSemillero()));

        validarPropiedadDelCoordinador(semillero, idCoordinador);

        Inscripcion aprobada = inscripcion
                .withEstado(Inscripcion.EstadoInscripcion.APROBADO)
                .withFechaActualizacion(LocalDateTime.now());

        Inscripcion guardada = inscripcionRepositoryPort.guardar(aprobada);

        semilleroIntegranteRepositoryPort.registrarIntegrante(
                inscripcion.getIdSemillero(),
                inscripcion.getNombres(),
                inscripcion.getApellidos(),
                inscripcion.getCedula(),
                inscripcion.getCorreo(),
                "ESTUDIANTE"
        );

        return guardada;
    }

    @Override
    @Transactional
    public Inscripcion rechazarInscripcion(Long idInscripcion, Long idCoordinador) {
        Inscripcion inscripcion = inscripcionRepositoryPort.buscarPorId(idInscripcion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción", idInscripcion));

        Semillero semillero = semilleroRepositoryPort.buscarPorId(inscripcion.getIdSemillero())
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", inscripcion.getIdSemillero()));

        validarPropiedadDelCoordinador(semillero, idCoordinador);

        Inscripcion rechazada = inscripcion
                .withEstado(Inscripcion.EstadoInscripcion.RECHAZADO)
                .withFechaActualizacion(LocalDateTime.now());

        return inscripcionRepositoryPort.guardar(rechazada);
    }
}
