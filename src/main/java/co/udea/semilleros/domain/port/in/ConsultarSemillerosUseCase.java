package co.udea.semilleros.domain.port.in;

import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.model.SemilleroFiltro;

public interface ConsultarSemillerosUseCase {

    PageResult<Semillero> listarSemillerosActivos(SemilleroFiltro filtro);

    Semillero obtenerDetalleSemillero(Long idSemillero);
}
