package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.port.out.OrganizacionSemilleroRepositoryPort;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrganizacionSemilleroRepositoryAdapter implements OrganizacionSemilleroRepositoryPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void guardarRecursos(Long idSemillero, List<Long> idsRecursos) {
        jdbcTemplate.update("DELETE FROM semillero_recurso WHERE id_semillero = ?", idSemillero);
        for (Long idRecurso : idsRecursos) {
            jdbcTemplate.update(
                    "INSERT INTO semillero_recurso (id_semillero, id_recurso) VALUES (?, ?)",
                    idSemillero, idRecurso);
        }
    }

    @Override
    public void guardarFuentesFinanciacion(Long idSemillero, List<Long> idsFuentes) {
        jdbcTemplate.update("DELETE FROM semillero_financiacion WHERE id_semillero = ?", idSemillero);
        for (Long idFuente : idsFuentes) {
            jdbcTemplate.update(
                    "INSERT INTO semillero_financiacion (id_semillero, id_fuente) VALUES (?, ?)",
                    idSemillero, idFuente);
        }
    }

    @Override
    public List<Long> obtenerIdsRecursosPorSemillero(Long idSemillero) {
        return jdbcTemplate.queryForList(
                "SELECT id_recurso FROM semillero_recurso WHERE id_semillero = ?",
                Long.class, idSemillero);
    }

    @Override
    public List<Long> obtenerIdsFuentesPorSemillero(Long idSemillero) {
        return jdbcTemplate.queryForList(
                "SELECT id_fuente FROM semillero_financiacion WHERE id_semillero = ?",
                Long.class, idSemillero);
    }
}
