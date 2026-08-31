package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.AreaOcde;
import co.udea.semilleros.domain.model.Campus;
import co.udea.semilleros.domain.model.UnidadAcademica;
import co.udea.semilleros.domain.port.out.FiltrosRepositoryPort;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.AreaOcdeEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.CampusEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.UnidadAcademicaEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.AreaOcdeJpaRepository;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.CampusJpaRepository;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.UnidadAcademicaJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FiltrosRepositoryAdapter - Pruebas unitarias")
class FiltrosRepositoryAdapterTest {

    @Mock private UnidadAcademicaJpaRepository unidadAcademicaJpaRepository;
    @Mock private AreaOcdeJpaRepository areaOcdeJpaRepository;
    @Mock private CampusJpaRepository campusJpaRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    private FiltrosRepositoryAdapter adapter() {
        return new FiltrosRepositoryAdapter(
                unidadAcademicaJpaRepository, areaOcdeJpaRepository, campusJpaRepository, jdbcTemplate);
    }

    @Test
    @DisplayName("listarTodasLasUnidades: debe mapear con campus asociado y sin campus")
    void listarTodasLasUnidades_conYSinCampus() {
        // ARRANGE
        UnidadAcademicaEntity conCampus = UnidadAcademicaEntity.builder()
                .id(1L).nombre("Ingeniería").siglas("ING")
                .campus(CampusEntity.builder().id(2L).nombre("Central").build())
                .build();
        UnidadAcademicaEntity sinCampus = UnidadAcademicaEntity.builder().id(3L).nombre("Salud").build();

        when(unidadAcademicaJpaRepository.findAll()).thenReturn(List.of(conCampus, sinCampus));

        // ACT
        List<UnidadAcademica> resultado = adapter().listarTodasLasUnidades();

        // ASSERT
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getIdCampus()).isEqualTo(2L);
        assertThat(resultado.get(1).getIdCampus()).isNull();
    }

    @Test
    @DisplayName("listarTodasLasAreas: debe mapear entidades a dominio")
    void listarTodasLasAreas_mapea() {
        when(areaOcdeJpaRepository.findAll()).thenReturn(
                List.of(AreaOcdeEntity.builder().id(1L).nombre("Biología").descripcion("desc").build()));

        List<AreaOcde> resultado = adapter().listarTodasLasAreas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Biología");
    }

    @Test
    @DisplayName("listarTodosLosCampus: debe mapear entidades a dominio")
    void listarTodosLosCampus_mapea() {
        when(campusJpaRepository.findAll()).thenReturn(
                List.of(CampusEntity.builder().id(1L).nombre("Central").ciudad("Medellín")
                        .departamento("Antioquia").direccion("Calle 1").build()));

        List<Campus> resultado = adapter().listarTodosLosCampus();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCiudad()).isEqualTo("Medellín");
    }

    @Test
    @DisplayName("listarTodosLosRecursos: debe ejecutar el RowMapper y construir los DTOs")
    void listarTodosLosRecursos_ejecutaRowMapper() throws SQLException {
        // ARRANGE
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id_recurso")).thenReturn(1L);
        when(rs.getString("nombre")).thenReturn("Laboratorio");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper<FiltrosRepositoryPort.RecursoDto> rowMapper = invocation.getArgument(1);
            return List.of(rowMapper.mapRow(rs, 0));
        });

        // ACT
        List<FiltrosRepositoryPort.RecursoDto> resultado = adapter().listarTodosLosRecursos();

        // ASSERT
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(1L);
        assertThat(resultado.get(0).nombre()).isEqualTo("Laboratorio");
    }

    @Test
    @DisplayName("listarTodasLasFuentes: debe ejecutar el RowMapper y construir los DTOs")
    void listarTodasLasFuentes_ejecutaRowMapper() throws SQLException {
        // ARRANGE
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id_fuente")).thenReturn(2L);
        when(rs.getString("nombre")).thenReturn("Universidad");

        ArgumentCaptor<RowMapper> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), captor.capture())).thenReturn(List.of());

        // ACT
        adapter().listarTodasLasFuentes();
        Object dto = captor.getValue().mapRow(rs, 0);

        // ASSERT
        FiltrosRepositoryPort.FuenteFinanciacionDto fuente = (FiltrosRepositoryPort.FuenteFinanciacionDto) dto;
        assertThat(fuente.id()).isEqualTo(2L);
        assertThat(fuente.nombre()).isEqualTo("Universidad");
    }
}
