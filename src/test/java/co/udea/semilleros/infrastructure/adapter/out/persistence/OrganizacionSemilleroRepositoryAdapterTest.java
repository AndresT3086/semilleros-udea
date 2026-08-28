package co.udea.semilleros.infrastructure.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizacionSemilleroRepositoryAdapter - Pruebas unitarias")
class OrganizacionSemilleroRepositoryAdapterTest {

    @Mock private JdbcTemplate jdbcTemplate;

    private OrganizacionSemilleroRepositoryAdapter adapter() {
        return new OrganizacionSemilleroRepositoryAdapter(jdbcTemplate);
    }

    @Test
    @DisplayName("guardarRecursos: debe borrar los previos e insertar cada recurso nuevo")
    void guardarRecursos_borraEInsertaCadaUno() {
        // ACT
        adapter().guardarRecursos(1L, List.of(10L, 20L, 30L));

        // ASSERT
        verify(jdbcTemplate).update(anyString(), eq(1L));
        verify(jdbcTemplate, times(3)).update(anyString(), eq(1L), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("guardarFuentesFinanciacion: debe borrar las previas e insertar cada fuente nueva")
    void guardarFuentesFinanciacion_borraEInsertaCadaUna() {
        adapter().guardarFuentesFinanciacion(1L, List.of(5L));

        verify(jdbcTemplate).update(anyString(), eq(1L));
        verify(jdbcTemplate).update(anyString(), eq(1L), eq(5L));
    }

    @Test
    @DisplayName("obtenerIdsRecursosPorSemillero: delega en queryForList")
    void obtenerIdsRecursosPorSemillero_delega() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(1L))).thenReturn(List.of(10L, 20L));

        List<Long> resultado = adapter().obtenerIdsRecursosPorSemillero(1L);

        assertThat(resultado).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("obtenerIdsFuentesPorSemillero: delega en queryForList")
    void obtenerIdsFuentesPorSemillero_delega() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(1L))).thenReturn(List.of(5L));

        List<Long> resultado = adapter().obtenerIdsFuentesPorSemillero(1L);

        assertThat(resultado).containsExactly(5L);
    }
}
