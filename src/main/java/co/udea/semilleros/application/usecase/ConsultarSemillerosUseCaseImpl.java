package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.RecursoNoEncontradoException;
import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.model.SemilleroFiltro;
import co.udea.semilleros.domain.port.in.ConsultarSemillerosUseCase;
import co.udea.semilleros.domain.port.out.SemilleroRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultarSemillerosUseCaseImpl implements ConsultarSemillerosUseCase {

    private final SemilleroRepositoryPort semilleroRepositoryPort;

    @Override
    public PageResult<Semillero> listarSemillerosActivos(SemilleroFiltro filtro) {
        return semilleroRepositoryPort.buscarActivos(filtro);
    }

    @Override
    public Semillero obtenerDetalleSemillero(Long idSemillero) {
        return semilleroRepositoryPort.buscarPorId(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", idSemillero));
    }
}
