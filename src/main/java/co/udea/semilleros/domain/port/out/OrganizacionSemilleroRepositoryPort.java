package co.udea.semilleros.domain.port.out;

import java.util.List;

public interface OrganizacionSemilleroRepositoryPort {

    void guardarRecursos(Long idSemillero, List<Long> idsRecursos);
    void guardarFuentesFinanciacion(Long idSemillero, List<Long> idsFuentes);

    List<Long> obtenerIdsRecursosPorSemillero(Long idSemillero);
    List<Long> obtenerIdsFuentesPorSemillero(Long idSemillero);
}
