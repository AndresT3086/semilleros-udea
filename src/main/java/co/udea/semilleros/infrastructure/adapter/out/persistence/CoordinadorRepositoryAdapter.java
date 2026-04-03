package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.Coordinador;
import co.udea.semilleros.domain.port.out.CoordinadorRepositoryPort;
import co.udea.semilleros.infrastructure.adapter.out.persistence.mapper.CoordinadorEntityMapper;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.CoordinadorJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CoordinadorRepositoryAdapter implements CoordinadorRepositoryPort {

    private final CoordinadorJpaRepository coordinadorJpaRepository;
    private final CoordinadorEntityMapper coordinadorEntityMapper;

    @Override
    public Optional<Coordinador> buscarPorCorreo(String correo) {
        return coordinadorJpaRepository.findByCorreo(correo)
                .map(coordinadorEntityMapper::toDomain);
    }

    @Override
    public Optional<Coordinador> buscarPorId(Long id) {
        return coordinadorJpaRepository.findById(id)
                .map(coordinadorEntityMapper::toDomain);
    }

    @Override
    public Coordinador guardar(Coordinador coordinador) {
        return coordinadorEntityMapper.toDomain(
                coordinadorJpaRepository.save(coordinadorEntityMapper.toEntity(coordinador)));
    }
}
