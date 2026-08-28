package co.udea.semilleros.infrastructure.security.jwt;

import co.udea.semilleros.domain.exception.TokenInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider - Pruebas unitarias")
class JwtTokenProviderTest {

    private static final String SECRETO = "una-clave-secreta-de-prueba-con-mas-de-32-caracteres";

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", SECRETO);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 86400000L);
        jwtTokenProvider.init();
    }

    // ─── generarToken / extraerCorreo / extraerIdCoordinador ──────────────────

    @Test
    @DisplayName("generarToken: debe permitir recuperar correo e id de coordinador desde el token generado")
    void generarToken_luegoExtraerDatos_retornaValoresOriginales() {
        // ARRANGE
        Long idCoordinador = 42L;
        String correo = "coordinador@udea.edu.co";

        // ACT
        String token = jwtTokenProvider.generarToken(idCoordinador, correo);

        // ASSERT
        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.extraerCorreo(token)).isEqualTo(correo);
        assertThat(jwtTokenProvider.extraerIdCoordinador(token)).isEqualTo(idCoordinador);
    }

    // ─── esTokenValido ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("esTokenValido: debe retornar true para un token recién generado con la misma clave")
    void esTokenValido_conTokenValido_retornaTrue() {
        // ARRANGE
        String token = jwtTokenProvider.generarToken(1L, "correo@udea.edu.co");

        // ACT & ASSERT
        assertThat(jwtTokenProvider.esTokenValido(token)).isTrue();
    }

    @Test
    @DisplayName("esTokenValido: debe retornar false (sin lanzar excepción) para un token firmado con otra clave")
    void esTokenValido_conFirmaDistinta_retornaFalse() {
        // ARRANGE: mismo token, pero provider con otra clave secreta
        JwtTokenProvider otroProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(otroProvider, "jwtSecret", "otra-clave-secreta-totalmente-distinta-32chars");
        ReflectionTestUtils.setField(otroProvider, "jwtExpirationMs", 86400000L);
        otroProvider.init();

        String tokenFirmadoConOtraClave = otroProvider.generarToken(1L, "correo@udea.edu.co");

        // ACT & ASSERT
        assertThat(jwtTokenProvider.esTokenValido(tokenFirmadoConOtraClave)).isFalse();
    }

    @Test
    @DisplayName("esTokenValido: debe retornar false (sin lanzar excepción) para un token corrupto")
    void esTokenValido_conTokenCorrupto_retornaFalse() {
        // ACT & ASSERT
        assertThat(jwtTokenProvider.esTokenValido("esto-no-es-un-jwt-valido")).isFalse();
    }

    @Test
    @DisplayName("esTokenValido: debe retornar false (sin lanzar excepción) para un token expirado")
    void esTokenValido_conTokenExpirado_retornaFalse() {
        // ARRANGE: provider cuya expiración ya quedó en el pasado
        JwtTokenProvider providerExpirado = new JwtTokenProvider();
        ReflectionTestUtils.setField(providerExpirado, "jwtSecret", SECRETO);
        ReflectionTestUtils.setField(providerExpirado, "jwtExpirationMs", -5000L);
        providerExpirado.init();

        String tokenExpirado = providerExpirado.generarToken(1L, "correo@udea.edu.co");

        // ACT & ASSERT
        assertThat(jwtTokenProvider.esTokenValido(tokenExpirado)).isFalse();
    }

    // ─── parsearClaims (indirectamente vía extraerCorreo) - excepciones ───────

    @Test
    @DisplayName("extraerCorreo: debe lanzar TokenInvalidoException cuando el token ya expiró")
    void extraerCorreo_conTokenExpirado_lanzaExcepcion() {
        // ARRANGE: provider cuya expiración ya quedó en el pasado
        JwtTokenProvider providerExpirado = new JwtTokenProvider();
        ReflectionTestUtils.setField(providerExpirado, "jwtSecret", SECRETO);
        ReflectionTestUtils.setField(providerExpirado, "jwtExpirationMs", -5000L);
        providerExpirado.init();

        String tokenExpirado = providerExpirado.generarToken(1L, "correo@udea.edu.co");

        // ACT & ASSERT
        assertThatThrownBy(() -> jwtTokenProvider.extraerCorreo(tokenExpirado))
                .isInstanceOf(TokenInvalidoException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    @DisplayName("extraerCorreo: debe lanzar TokenInvalidoException cuando la firma o estructura es inválida")
    void extraerCorreo_conTokenInvalido_lanzaExcepcion() {
        // ACT & ASSERT
        assertThatThrownBy(() -> jwtTokenProvider.extraerCorreo("token.invalido.corrupto"))
                .isInstanceOf(TokenInvalidoException.class);
    }
}
