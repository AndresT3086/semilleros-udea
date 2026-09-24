package co.udea.semilleros.domain.port.out;

import co.udea.semilleros.domain.model.Usuario;
import java.util.Optional;

public interface UsuarioRepositoryPort {

    Optional<Usuario> buscarPorCorreo(String correo);

    Optional<Usuario> buscarPorId(Long id);

    Usuario guardar(Usuario usuario);
}
