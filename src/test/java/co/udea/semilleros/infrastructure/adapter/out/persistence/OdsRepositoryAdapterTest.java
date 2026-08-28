package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.port.out.OdsRepositoryPort;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OdsRepositoryAdapter - Pruebas unitarias")
class OdsRepositoryAdapterTest {

    @Mock private JdbcTemplate jdbcTemplate;

    private OdsRepositoryAdapter adapter() {
        return new OdsRepositoryAdapter(jdbcTemplate);
    }

    @Test
    @DisplayName("guardarOds: debe actualizar la fila del semillero con los datos ODS")
    void guardarOds_actualizaFila() {
        // ACT
        adapter().guardarOds(1L, 3L, "Subárea", 7L, "Observaciones");

        // ASSERT
        verify(jdbcTemplate).update(anyString(), eq(3L), eq("Subárea"), eq(7L), eq("Observaciones"), eq(1L));
    }

    @Test
    @DisplayName("obtenerPorSemillero: debe ejecutar el RowMapper y retornar el DTO cuando hay resultado")
    void obtenerPorSemillero_conResultado_retornaDto() throws SQLException {
        // ARRANGE
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("id_area_ocde", Long.class)).thenReturn(3L);
        when(rs.getString("nombre_area")).thenReturn("Biología");
        when(rs.getString("subarea_ocde")).thenReturn("Subárea");
        when(rs.getObject("ods_principal", Long.class)).thenReturn(7L);
        when(rs.getString("nombre_ods")).thenReturn("Salud");
        when(rs.getString("observaciones_finales")).thenReturn("Obs");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L))).thenAnswer(invocation -> {
            RowMapper<OdsRepositoryPort.OdsDto> rowMapper = invocation.getArgument(1);
            return List.of(rowMapper.mapRow(rs, 0));
        });

        // ACT
        Optional<OdsRepositoryPort.OdsDto> resultado = adapter().obtenerPorSemillero(1L);

        // ASSERT
        assertThat(resultado).isPresent();
        assertThat(resultado.get().idAreaOcde()).isEqualTo(3L);
        assertThat(resultado.get().nombreOdsPrincipal()).isEqualTo("Salud");
    }

    @Test
    @DisplayName("obtenerPorSemillero: debe retornar vacío cuando no hay filas")
    void obtenerPorSemillero_sinResultado_retornaVacio() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(2L))).thenReturn(List.of());

        Optional<OdsRepositoryPort.OdsDto> resultado = adapter().obtenerPorSemillero(2L);

        assertThat(resultado).isEmpty();
    }
}
