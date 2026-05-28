package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.port.out.ProduccionAcademicaRepositoryPort;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.ProduccionAcademicaEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.ProduccionAcademicaJpaRepository;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.SemilleroJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProduccionAcademicaRepositoryAdapter implements ProduccionAcademicaRepositoryPort {


    private final ProduccionAcademicaJpaRepository produccionJpaRepository;
    private final SemilleroJpaRepository           semilleroJpaRepository;

    @Override
    public void guardarProduccionResumen(
            Long idSemillero,
            Boolean tienenArticulos,    Integer cantidadArticulos,
            Boolean tienenLibros,       Integer cantidadLibros,
            Boolean organizanEventos,   Integer cantidadEventos,
            Boolean participanEventos,  Integer cantidadParticipaciones) {

        // Buscar registro existente o crear uno nuevo
        ProduccionAcademicaEntity entity = produccionJpaRepository
                .findByIdSemillero(idSemillero)
                .orElse(ProduccionAcademicaEntity.builder()
                        .semillero(semilleroJpaRepository.getReferenceById(idSemillero))
                        .build());

        entity.setTienenArticulos(Boolean.TRUE.equals(tienenArticulos));
        entity.setCantidadArticulos(cantidadArticulos != null ? cantidadArticulos : 0);
        entity.setTienenLibros(Boolean.TRUE.equals(tienenLibros));
        entity.setCantidadLibros(cantidadLibros != null ? cantidadLibros : 0);
        entity.setOrganizanEventos(Boolean.TRUE.equals(organizanEventos));
        entity.setCantidadEventos(cantidadEventos != null ? cantidadEventos : 0);
        entity.setParticipaEnEventos(Boolean.TRUE.equals(participanEventos));
        entity.setCantidadParticipaciones(cantidadParticipaciones != null ? cantidadParticipaciones : 0);

        produccionJpaRepository.save(entity);
    }
}
