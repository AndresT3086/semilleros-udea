package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.CredencialesInvalidasException;
import co.udea.semilleros.domain.exception.DominioCorreoNoPermitidoException;
import co.udea.semilleros.domain.exception.ValidacionBotException;
import co.udea.semilleros.domain.model.Coordinador;
import co.udea.semilleros.domain.port.in.AutenticarCoordinadorUseCase;
import co.udea.semilleros.domain.port.out.CoordinadorRepositoryPort;
import co.udea.semilleros.infrastructure.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutenticarCoordinadorUseCaseImpl implements AutenticarCoordinadorUseCase {

    private final CoordinadorRepositoryPort coordinadorRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.security.allowed-email-domain}")
    private String dominioPermitido;

    @Override
    public String autenticar(String correo, String password, int respuestaMath, int operando1, int operando2) {
        validarDominioCorreo(correo);
        validarOperacionMatematica(respuestaMath, operando1, operando2);

        Coordinador coordinador = coordinadorRepositoryPort.buscarPorCorreo(correo)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!coordinador.getActivo()) {
            throw new CredencialesInvalidasException();
        }

        if (!passwordEncoder.matches(password, coordinador.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }

        return jwtTokenProvider.generarToken(coordinador.getId(), coordinador.getCorreo());
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
