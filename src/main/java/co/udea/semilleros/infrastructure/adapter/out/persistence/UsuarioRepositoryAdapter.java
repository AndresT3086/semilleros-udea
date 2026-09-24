package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.Usuario;
import co.udea.semilleros.domain.port.out.UsuarioRepositoryPort;
import co.udea.semilleros.infrastructure.adapter.out.persistence.mapper.UsuarioEntityMapper;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository usuarioJpaRepository;
    private final UsuarioEntityMapper usuarioEntityMapper;

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioJpaRepository.findByCorreo(correo)
                .map(usuarioEntityMapper::toDomain);
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioJpaRepository.findById(id)
                .map(usuarioEntityMapper::toDomain);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return usuarioEntityMapper.toDomain(
                usuarioJpaRepository.save(usuarioEntityMapper.toEntity(usuario)));
    }
}
