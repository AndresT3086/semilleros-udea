package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.port.out.OdsRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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

    @Override
    public Optional<OdsDto> obtenerPorSemillero(Long idSemillero) {
        List<OdsRepositoryPort.OdsDto> result = jdbcTemplate.query("""
            SELECT s.id_area_ocde, ao.nombre AS nombre_area,
                   s.subarea_ocde,
                   s.ods_principal, o.nombre AS nombre_ods,
                   s.observaciones_finales
            FROM semillero s
            LEFT JOIN area_ocde ao ON ao.id_area = s.id_area_ocde
            LEFT JOIN ods o        ON o.id_ods   = s.ods_principal
            WHERE s.id_semillero = ?
            """,
                (rs, rowNum) -> new OdsRepositoryPort.OdsDto(
                        rs.getObject("id_area_ocde", Long.class),
                        rs.getString("nombre_area"),
                        rs.getString("subarea_ocde"),
                        rs.getObject("ods_principal", Long.class),
                        rs.getString("nombre_ods"),
                        rs.getString("observaciones_finales")
                ),
                idSemillero);

        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }
}
