package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.port.out.ActividadesRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ActividadesRepositoryAdapter implements ActividadesRepositoryPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void actualizarActividades(Long idSemillero, List<ActividadDto> actividades) {
        for (ActividadDto dto : actividades) {
            jdbcTemplate.update("""
                INSERT INTO semillero_actividad (id_semillero, id_actividad, realiza)
                VALUES (?, ?, ?)
                ON CONFLICT (id_semillero, id_actividad)
                DO UPDATE SET realiza = EXCLUDED.realiza
                """, idSemillero, dto.idActividad(), dto.realiza());
        }
    }
}
