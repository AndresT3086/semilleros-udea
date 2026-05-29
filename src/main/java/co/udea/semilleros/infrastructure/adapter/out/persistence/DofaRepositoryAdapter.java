package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.port.out.DofaRepositoryPort;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.DofaEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.DofaJpaRepository;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.SemilleroJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DofaRepositoryAdapter  implements DofaRepositoryPort {

    private final DofaJpaRepository dofaJpaRepository;
    private final SemilleroJpaRepository semilleroJpaRepository;

    @Override
    public void guardarDofa(Long idSemillero, String fortalezas, String debilidades,
                            String oportunidades, String amenazas) {
        dofaJpaRepository.deleteBySemilleroId(idSemillero);

        List<DofaEntity> entidades = List.of(
                DofaEntity.builder().semillero(semilleroJpaRepository.getReferenceById(idSemillero))
                        .tipo("FORTALEZA").descripcion(fortalezas).build(),
                DofaEntity.builder().semillero(semilleroJpaRepository.getReferenceById(idSemillero))
                        .tipo("DEBILIDAD").descripcion(debilidades).build(),
                DofaEntity.builder().semillero(semilleroJpaRepository.getReferenceById(idSemillero))
                        .tipo("OPORTUNIDAD").descripcion(oportunidades).build(),
                DofaEntity.builder().semillero(semilleroJpaRepository.getReferenceById(idSemillero))
                        .tipo("AMENAZA").descripcion(amenazas).build()
        );

        dofaJpaRepository.saveAll(entidades);
    }

    @Override
    public Optional<DofaDto> obtenerPorSemillero(Long idSemillero) {
        List<DofaEntity> items = dofaJpaRepository.findBySemilleroId(idSemillero);

        if (items.isEmpty()) return Optional.empty();

        String fortalezas    = extraer(items, "FORTALEZA");
        String debilidades   = extraer(items, "DEBILIDAD");
        String oportunidades = extraer(items, "OPORTUNIDAD");
        String amenazas      = extraer(items, "AMENAZA");

        return Optional.of(new DofaRepositoryPort.DofaDto(
                fortalezas, debilidades, oportunidades, amenazas));
    }

    private String extraer(List<DofaEntity> items, String tipo) {
        return items.stream()
                .filter(d -> tipo.equals(d.getTipo()))
                .map(DofaEntity::getDescripcion)
                .findFirst()
                .orElse(null);
    }
}
