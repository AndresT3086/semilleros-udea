package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.port.out.OdsRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OdsRepositoryAdapter implements OdsRepositoryPort {


    private final JdbcTemplate jdbcTemplate;

    @Override
    public void guardarOds(Long idSemillero, Long idAreaOcde, String subAreaOcde,
                           Long idOdsPrincipal, String observacionesFinales) {
        jdbcTemplate.update("""
            UPDATE semillero
               SET id_area_ocde          = ?,
                   subarea_ocde          = ?,
                   ods_principal         = ?,
                   observaciones_finales = ?,
                   fecha_actualizacion   = NOW()
             WHERE id_semillero = ?
            """,
                idAreaOcde, subAreaOcde, idOdsPrincipal, observacionesFinales, idSemillero);
    }
}
