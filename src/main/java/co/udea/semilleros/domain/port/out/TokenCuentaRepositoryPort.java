package co.udea.semilleros.domain.port.out;

import co.udea.semilleros.domain.model.acceso.TokenCuenta;

import java.time.Instant;
import java.util.Optional;

public interface TokenCuentaRepositoryPort {

    /** Crea un token de activación e invalida los anteriores del mismo usuario. */
    void crear(Long idUsuario, String tokenHash, TokenCuenta.OrigenToken origen, Instant expira);

    /** Token sin usar y sin vencer. */
    Optional<TokenCuenta> buscarVigente(String tokenHash, Instant ahora);

    void marcarUsado(Long idToken);

    /** Borra los tokens usados o vencidos antes del límite. */
    int eliminarAnterioresA(Instant limite);
}
