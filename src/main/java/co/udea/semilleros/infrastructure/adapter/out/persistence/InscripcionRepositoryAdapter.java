package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.port.out.InscripcionRepositoryPort;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.InscripcionEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.mapper.InscripcionEntityMapper;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.InscripcionJpaRepository;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.SemilleroJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InscripcionRepositoryAdapter implements InscripcionRepositoryPort {

    private final InscripcionJpaRepository inscripcionJpaRepository;
    private final InscripcionEntityMapper inscripcionEntityMapper;
    private final SemilleroJpaRepository semilleroJpaRepository;

    @Override
    public Inscripcion guardar(Inscripcion inscripcion) {
        InscripcionEntity entity = inscripcionEntityMapper.toEntity(inscripcion);

        SemilleroEntity semilleroRef = semilleroJpaRepository.getReferenceById(inscripcion.getIdSemillero());
        entity.setSemillero(semilleroRef);

        return inscripcionEntityMapper.toDomain(inscripcionJpaRepository.save(entity));
    }

    @Override
    public boolean existeInscripcionActivaPorCorreoYSemillero(String correo, Long idSemillero) {
        return inscripcionJpaRepository
                .existeInscripcionActivaPorCorreoYSemillero(correo, idSemillero);
    }

    @Override
    public Optional<Inscripcion> buscarPorId(Long id) {
        return inscripcionJpaRepository.findById(id)
                .map(inscripcionEntityMapper::toDomain);
    }

    @Override
    public List<Inscripcion> buscarPorSemilleroYEstado(Long idSemillero,
                                                       Inscripcion.EstadoInscripcion estado) {
        return inscripcionJpaRepository
                .findBySemilleroIdAndEstado(
                        idSemillero,
                        InscripcionEntity.EstadoInscripcionJpa.valueOf(estado.name()))
                .stream()
                .map(inscripcionEntityMapper::toDomain)
                .toList();
    }
}
