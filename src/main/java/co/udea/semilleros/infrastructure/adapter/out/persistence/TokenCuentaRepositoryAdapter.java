package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.acceso.TokenCuenta;
import co.udea.semilleros.domain.port.out.TokenCuentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Types;
import java.time.Instant;
import java.util.Optional;

import static co.udea.semilleros.infrastructure.adapter.out.persistence.SolicitudAccesoRepositoryAdapter.fecha;
import static co.udea.semilleros.infrastructure.adapter.out.persistence.SolicitudAccesoRepositoryAdapter.instante;

@Component
@RequiredArgsConstructor
public class TokenCuentaRepositoryAdapter implements TokenCuentaRepositoryPort {

    private static final String INVALIDAR_ANTERIORES =
            "UPDATE token_cuenta SET usado = TRUE WHERE id_usuario = :usuario AND usado = FALSE";

    private static final String INSERTAR = """
            INSERT INTO token_cuenta (id_usuario, token_hash, origen, expira)
            VALUES (:usuario, :hash, :origen, :expira)
            """;

    private static final String VIGENTE = """
            SELECT id_token, id_usuario, origen, expira, usado FROM token_cuenta
            WHERE token_hash = :hash AND usado = FALSE AND expira > :ahora
            """;

    private static final String MARCAR_USADO = "UPDATE token_cuenta SET usado = TRUE WHERE id_token = :id";

    private static final String ELIMINAR_ANTERIORES =
            "DELETE FROM token_cuenta WHERE expira < :limite OR (usado = TRUE AND fecha_creacion < :limite)";

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void crear(Long idUsuario, String tokenHash, TokenCuenta.OrigenToken origen, Instant expira) {
        jdbc.update(INVALIDAR_ANTERIORES, new MapSqlParameterSource("usuario", idUsuario));
        jdbc.update(INSERTAR, new MapSqlParameterSource()
                .addValue("usuario", idUsuario, Types.BIGINT)
                .addValue("hash", tokenHash, Types.VARCHAR)
                .addValue("origen", origen.name(), Types.VARCHAR)
                .addValue("expira", fecha(expira), Types.TIMESTAMP_WITH_TIMEZONE));
    }

    @Override
    public Optional<TokenCuenta> buscarVigente(String tokenHash, Instant ahora) {
        return jdbc.query(VIGENTE, new MapSqlParameterSource()
                        .addValue("hash", tokenHash, Types.VARCHAR)
                        .addValue("ahora", fecha(ahora), Types.TIMESTAMP_WITH_TIMEZONE),
                (rs, i) -> new TokenCuenta(
                        rs.getLong("id_token"),
                        rs.getLong("id_usuario"),
                        TokenCuenta.OrigenToken.valueOf(rs.getString("origen")),
                        instante(rs, "expira"),
                        rs.getBoolean("usado"))).stream().findFirst();
    }

    @Override
    public void marcarUsado(Long idToken) {
        jdbc.update(MARCAR_USADO, new MapSqlParameterSource("id", idToken));
    }

    @Override
    public int eliminarAnterioresA(Instant limite) {
        return jdbc.update(ELIMINAR_ANTERIORES, new MapSqlParameterSource()
                .addValue("limite", fecha(limite), Types.TIMESTAMP_WITH_TIMEZONE));
    }
}
