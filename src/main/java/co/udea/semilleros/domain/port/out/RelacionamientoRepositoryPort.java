package co.udea.semilleros.domain.port.out;

import java.util.Optional;

public interface RelacionamientoRepositoryPort {

    void guardarRelacionamiento(
            Long    idSemillero,
            Boolean adscritoGrupo,
            String  grupoInvestigacion,
            String  relacionGrupo,
            String  centroInvestigaciones,
            String  relacionCentro,
            String  departamento,
            String  relacionDepartamento,
            String  facultad,
            String  relacionFacultad
    );

    Optional<RelacionamientoDto> obtenerPorSemillero(Long idSemillero);

    record RelacionamientoDto(
            Boolean adscritoGrupo,
            String grupoInvestigacion, String relacionGrupo,
            String centroInvestigaciones, String relacionCentro,
            String departamento, String relacionDepartamento,
            String facultad, String relacionFacultad
    ) {}
}
