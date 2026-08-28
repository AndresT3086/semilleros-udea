package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.port.out.ActividadesRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActividadesRepositoryAdapter - Pruebas unitarias")
class ActividadesRepositoryAdapterTest {

    @Mock private JdbcTemplate jdbcTemplate;

    private ActividadesRepositoryAdapter adapter() {
        return new ActividadesRepositoryAdapter(jdbcTemplate);
    }

    @Test
    @DisplayName("actualizarActividades: debe hacer upsert por cada actividad recibida")
    void actualizarActividades_haceUpsertPorCadaUna() {
        // ACT
        adapter().actualizarActividades(1L, List.of(
                new ActividadesRepositoryPort.ActividadDto(10L, true),
                new ActividadesRepositoryPort.ActividadDto(20L, false)
        ));

        // ASSERT
        verify(jdbcTemplate).update(anyString(), eq(1L), eq(10L), eq(true));
        verify(jdbcTemplate).update(anyString(), eq(1L), eq(20L), eq(false));
        verify(jdbcTemplate, times(2)).update(anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("obtenerTodasConEstadoPorSemillero: debe ejecutar el RowMapper y construir los DTOs")
    void obtenerTodasConEstadoPorSemillero_ejecutaRowMapper() throws SQLException {
        // ARRANGE
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id_actividad")).thenReturn(1L);
        when(rs.getString("nombre")).thenReturn("Publicación");
        when(rs.getString("categoria")).thenReturn("DIFUSION");
        when(rs.getBoolean("realiza")).thenReturn(true);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L))).thenAnswer(invocation -> {
            RowMapper<ActividadesRepositoryPort.ActividadDetalleDto> rowMapper = invocation.getArgument(1);
            return List.of(rowMapper.mapRow(rs, 0));
        });

        // ACT
        List<ActividadesRepositoryPort.ActividadDetalleDto> resultado =
                adapter().obtenerTodasConEstadoPorSemillero(1L);

        // ASSERT
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).idActividad()).isEqualTo(1L);
        assertThat(resultado.get(0).nombre()).isEqualTo("Publicación");
        assertThat(resultado.get(0).categoria()).isEqualTo("DIFUSION");
        assertThat(resultado.get(0).realiza()).isTrue();
    }
}
