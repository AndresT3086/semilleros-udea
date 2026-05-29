package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.AreaOcde;
import co.udea.semilleros.domain.model.Campus;
import co.udea.semilleros.domain.model.UnidadAcademica;
import co.udea.semilleros.domain.port.out.FiltrosRepositoryPort;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.AreaOcdeJpaRepository;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.CampusJpaRepository;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.UnidadAcademicaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FiltrosRepositoryAdapter implements FiltrosRepositoryPort {

    private final UnidadAcademicaJpaRepository unidadAcademicaJpaRepository;
    private final AreaOcdeJpaRepository areaOcdeJpaRepository;
    private final CampusJpaRepository campusJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<UnidadAcademica> listarTodasLasUnidades() {
        return unidadAcademicaJpaRepository.findAll().stream()
                .map(e -> UnidadAcademica.builder()
                        .id(e.getId())
                        .nombre(e.getNombre())
                        .siglas(e.getSiglas())
                        .idCampus(e.getCampus() != null ? e.getCampus().getId() : null)
                        .build())
                .toList();
    }

    @Override
    public List<AreaOcde> listarTodasLasAreas() {
        return areaOcdeJpaRepository.findAll().stream()
                .map(e -> AreaOcde.builder()
                        .id(e.getId())
                        .nombre(e.getNombre())
                        .descripcion(e.getDescripcion())
                        .build())
                .toList();
    }

    @Override
    public List<Campus> listarTodosLosCampus() {
        return campusJpaRepository.findAll().stream()
                .map(e -> Campus.builder()
                        .id(e.getId())
                        .nombre(e.getNombre())
                        .ciudad(e.getCiudad())
                        .departamento(e.getDepartamento())
                        .direccion(e.getDireccion())
                        .build())
                .toList();
    }

    @Override
    public List<FiltrosRepositoryPort.RecursoDto> listarTodosLosRecursos() {
        return jdbcTemplate.query(
                "SELECT id_recurso, nombre FROM recurso ORDER BY nombre",
                (rs, rowNum) -> new FiltrosRepositoryPort.RecursoDto(
                        rs.getLong("id_recurso"),
                        rs.getString("nombre")
                ));
    }

    @Override
    public List<FiltrosRepositoryPort.FuenteFinanciacionDto> listarTodasLasFuentes() {
        return jdbcTemplate.query(
                "SELECT id_fuente, nombre FROM fuente_financiacion ORDER BY nombre",
                (rs, rowNum) -> new FiltrosRepositoryPort.FuenteFinanciacionDto(
                        rs.getLong("id_fuente"),
                        rs.getString("nombre")
                ));
    }
}
