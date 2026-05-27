package co.udea.semilleros.domain.port.out;

import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.model.SemilleroFiltro;

import java.util.List;
import java.util.Optional;

public interface SemilleroRepositoryPort {

    PageResult<Semillero> buscarActivos(SemilleroFiltro filtro);

    Optional<Semillero> buscarPorId(Long id);

    Optional<Semillero> buscarPorCodigo(String codigo);

    List<Semillero> buscarPorCoordinador(Long idCoordinador);

    List<Semillero> buscarPorCoordinadorYEstados(Long idCoordinador, List<Semillero.EstadoSemillero> estados);


    Semillero guardar(Semillero semillero);

    boolean existePorNombre(String nombre);

    boolean existePorCodigo(String codigo);

    long contarPorEstado(Semillero.EstadoSemillero estado);

}
