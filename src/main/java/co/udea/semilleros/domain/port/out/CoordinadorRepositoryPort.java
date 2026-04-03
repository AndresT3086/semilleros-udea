package co.udea.semilleros.domain.port.out;

import co.udea.semilleros.domain.model.Coordinador;
import java.util.Optional;

public interface CoordinadorRepositoryPort {

    Optional<Coordinador> buscarPorCorreo(String correo);

    Optional<Coordinador> buscarPorId(Long id);

    Coordinador guardar(Coordinador coordinador);
}
