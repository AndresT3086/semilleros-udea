package co.udea.semilleros.domain.port.in;

import co.udea.semilleros.domain.model.AreaOcde;
import co.udea.semilleros.domain.model.Campus;
import co.udea.semilleros.domain.model.UnidadAcademica;

import java.util.List;

public interface ConsultarFiltrosUseCase {

    List<UnidadAcademica> listarUnidadesAcademicas();

    List<AreaOcde> listarAreasOcde();

    List<Campus> listarCampus();
}
