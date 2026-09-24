package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.CredencialesInvalidasException;
import co.udea.semilleros.domain.exception.DominioCorreoNoPermitidoException;
import co.udea.semilleros.domain.exception.ValidacionBotException;
import co.udea.semilleros.domain.model.Usuario;
import co.udea.semilleros.domain.port.in.AutenticarUsuarioUseCase;
import co.udea.semilleros.domain.port.out.UsuarioRepositoryPort;
import co.udea.semilleros.infrastructure.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutenticarUsuarioUseCaseImpl implements AutenticarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.security.allowed-email-domain}")
    private String dominioPermitido;

    @Override
    public String autenticar(String correo, String password, int respuestaMath, int operando1, int operando2) {
        validarDominioCorreo(correo);
        validarOperacionMatematica(respuestaMath, operando1, operando2);

        Usuario usuario = usuarioRepositoryPort.buscarPorCorreo(correo)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!usuario.getActivo()) {
            throw new CredencialesInvalidasException();
        }

        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }

        return jwtTokenProvider.generarToken(usuario.getId(), usuario.getCorreo(), usuario.getRol());
    }

    private void validarDominioCorreo(String correo) {
        if (correo == null || !correo.toLowerCase().endsWith(dominioPermitido)) {
            throw new DominioCorreoNoPermitidoException(correo);
        }
    }

    private void validarOperacionMatematica(int respuesta, int operando1, int operando2) {
        if (respuesta != (operando1 + operando2)) {
            throw new ValidacionBotException();
        }
    }
}
