package co.udea.semilleros.domain.port.out;

import co.udea.semilleros.domain.model.AreaOcde;
import co.udea.semilleros.domain.model.Campus;
import co.udea.semilleros.domain.model.UnidadAcademica;

import java.util.List;

public interface FiltrosRepositoryPort {

    List<UnidadAcademica> listarTodasLasUnidades();

    List<AreaOcde> listarTodasLasAreas();

    List<Campus> listarTodosLosCampus();

}
