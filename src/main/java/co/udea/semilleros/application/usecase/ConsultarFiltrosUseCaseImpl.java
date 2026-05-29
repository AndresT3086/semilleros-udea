package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.model.AreaOcde;
import co.udea.semilleros.domain.model.Campus;
import co.udea.semilleros.domain.model.UnidadAcademica;
import co.udea.semilleros.domain.port.in.ConsultarFiltrosUseCase;
import co.udea.semilleros.domain.port.out.FiltrosRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultarFiltrosUseCaseImpl implements ConsultarFiltrosUseCase {

    private final FiltrosRepositoryPort filtrosRepositoryPort;

    @Override
    @Cacheable("filtros-unidades")
    public List<UnidadAcademica> listarUnidadesAcademicas() {
        return filtrosRepositoryPort.listarTodasLasUnidades();
    }

    @Override
    @Cacheable("filtros-campus")
    public List<Campus> listarCampus() {
        return filtrosRepositoryPort.listarTodosLosCampus();
    }

    @Override
    @Cacheable("filtros-areas-ocde")
    public List<AreaOcde> listarAreasOcde() {
        return filtrosRepositoryPort.listarTodasLasAreas();
    }
}
