package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.InscripcionDuplicadaException;
import co.udea.semilleros.domain.exception.RecursoNoEncontradoException;
import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.port.in.InscribirseASemilleroUseCase;
import co.udea.semilleros.domain.port.out.CoordinadorRepositoryPort;
import co.udea.semilleros.domain.port.out.InscripcionRepositoryPort;
import co.udea.semilleros.domain.port.out.NotificacionEmailPort;
import co.udea.semilleros.domain.port.out.SemilleroRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InscribirseASemilleroUseCaseImpl implements InscribirseASemilleroUseCase {

    private final InscripcionRepositoryPort inscripcionRepositoryPort;
    private final SemilleroRepositoryPort semilleroRepositoryPort;
    private final CoordinadorRepositoryPort coordinadorRepositoryPort;
    private final NotificacionEmailPort notificacionEmailPort;

    @Override
    @Transactional
    public Inscripcion inscribir(Inscripcion inscripcion) {

        Semillero semillero = semilleroRepositoryPort.buscarPorId(inscripcion.getIdSemillero())
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", inscripcion.getIdSemillero()));

        if (inscripcionRepositoryPort.existeInscripcionActivaPorCorreoYSemillero(
                inscripcion.getCorreo(), inscripcion.getIdSemillero())) {
            throw new InscripcionDuplicadaException(inscripcion.getCorreo(), inscripcion.getIdSemillero());
        }

        Inscripcion nueva = inscripcion
                .withEstado(Inscripcion.EstadoInscripcion.PENDIENTE)
                .withFechaInscripcion(LocalDateTime.now())
                .withNombreSemillero(semillero.getNombre());

        Inscripcion guardada = inscripcionRepositoryPort.guardar(nueva);

        coordinadorRepositoryPort.buscarPorId(semillero.getIdCoordinador())
                .ifPresent(coordinador ->
                        notificacionEmailPort.notificarNuevaInscripcion(guardada, coordinador.getCorreo()));

        return guardada;
    }
}
