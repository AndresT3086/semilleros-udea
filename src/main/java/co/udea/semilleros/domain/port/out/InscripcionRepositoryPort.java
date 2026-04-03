package co.udea.semilleros.domain.port.out;

import co.udea.semilleros.domain.model.Inscripcion;

import java.util.Optional;

public interface InscripcionRepositoryPort {

    Inscripcion guardar(Inscripcion inscripcion);

    boolean existeInscripcionActivaPorCorreoYSemillero(String correo, Long idSemillero);

    Optional<Inscripcion> buscarPorId(Long id);

}
