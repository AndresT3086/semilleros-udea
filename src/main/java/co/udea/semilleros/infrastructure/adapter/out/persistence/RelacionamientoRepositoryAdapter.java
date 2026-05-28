package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.port.out.RelacionamientoRepositoryPort;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroRelacionamientoEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.SemilleroJpaRepository;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.SemilleroRelacionamientoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RelacionamientoRepositoryAdapter implements RelacionamientoRepositoryPort {

    private final SemilleroRelacionamientoJpaRepository relacionamientoJpaRepository;
    private final SemilleroJpaRepository                semilleroJpaRepository;

    @Override
    public void guardarRelacionamiento(
            Long idSemillero, Boolean adscritoGrupo,
            String grupoInvestigacion, String relacionGrupo,
            String centroInvestigaciones, String relacionCentro,
            String departamento, String relacionDepartamento,
            String facultad, String relacionFacultad) {

        SemilleroRelacionamientoEntity entity = relacionamientoJpaRepository
                .findByIdSemillero(idSemillero)
                .orElse(SemilleroRelacionamientoEntity.builder()
                        .semillero(semilleroJpaRepository.getReferenceById(idSemillero))
                        .build());

        entity.setAdscritoGrupo(Boolean.TRUE.equals(adscritoGrupo));
        entity.setGrupoInvestigacion(grupoInvestigacion);
        entity.setRelacionGrupo(relacionGrupo);
        entity.setCentroInvestigaciones(centroInvestigaciones);
        entity.setRelacionCentro(relacionCentro);
        entity.setDepartamento(departamento);
        entity.setRelacionDepartamento(relacionDepartamento);
        entity.setFacultad(facultad);
        entity.setRelacionFacultad(relacionFacultad);

        relacionamientoJpaRepository.save(entity);
    }
}
