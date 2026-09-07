package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.CredencialesInvalidasException;
import co.udea.semilleros.domain.exception.DominioCorreoNoPermitidoException;
import co.udea.semilleros.domain.exception.ValidacionBotException;
import co.udea.semilleros.domain.model.Coordinador;
import co.udea.semilleros.domain.port.out.CoordinadorRepositoryPort;
import co.udea.semilleros.infrastructure.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutenticarCoordinadorUseCase - Pruebas unitarias")
class AutenticarCoordinadorUseCaseImplTest {

    @Mock
    private CoordinadorRepositoryPort coordinadorRepositoryPort;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AutenticarCoordinadorUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "dominioPermitido", "@udea.edu.co");
    }

    // ─── autenticar - caso exitoso ─────────────────────────────────────────────

    @Test
    @DisplayName("autenticar: debe retornar token JWT cuando las credenciales son válidas")
    void autenticar_conCredencialesValidas_retornaToken() {
        // ARRANGE
        String correo = "coordinador@udea.edu.co";
        String password = "clave-de-prueba-no-real";
        String tokenEsperado = "eyJhbGciOiJIUzI1NiJ9.test.token";

        Coordinador coordinador = Coordinador.builder()
                .id(1L)
                .correo(correo)
                .passwordHash("hash-mockeado-de-prueba")
                .rol("COORDINADOR")
                .activo(true)
                .build();

        when(coordinadorRepositoryPort.buscarPorCorreo(correo)).thenReturn(Optional.of(coordinador));
        when(passwordEncoder.matches(eq(password), any())).thenReturn(true);
        when(jwtTokenProvider.generarToken(1L, correo, "COORDINADOR")).thenReturn(tokenEsperado);

        // ACT
        String tokenResultado = useCase.autenticar(correo, password, 7, 3, 4);

        // ASSERT
        assertThat(tokenResultado).isEqualTo(tokenEsperado);
    }

    // ─── autenticar - dominio de correo ───────────────────────────────────────

    @Test
    @DisplayName("autenticar: debe lanzar DominioCorreoNoPermitidoException para correo no institucional")
    void autenticar_conCorreoNoInstitucional_lanzaExcepcion() {
        // ARRANGE
        String correoExterno = "coordinador@gmail.com";

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.autenticar(correoExterno, "pass", 5, 2, 3))
                .isInstanceOf(DominioCorreoNoPermitidoException.class)
                .hasMessageContaining("gmail.com");
    }

    // ─── autenticar - validación bot ──────────────────────────────────────────

    @Test
    @DisplayName("autenticar: debe lanzar ValidacionBotException cuando la operación matemática es incorrecta")
    void autenticar_conOperacionMatematicaIncorrecta_lanzaExcepcion() {
        // ARRANGE
        String correo = "coordinador@udea.edu.co";
        int op1 = 5, op2 = 3, respuestaIncorrecta = 10; // 5+3=8, no 10

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.autenticar(correo, "pass", respuestaIncorrecta, op1, op2))
                .isInstanceOf(ValidacionBotException.class);
    }

    // ─── autenticar - credenciales inválidas ──────────────────────────────────

    @Test
    @DisplayName("autenticar: debe lanzar CredencialesInvalidasException cuando el correo no existe")
    void autenticar_conCorreoInexistente_lanzaExcepcion() {
        // ARRANGE
        String correo = "noexiste@udea.edu.co";
        when(coordinadorRepositoryPort.buscarPorCorreo(correo)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.autenticar(correo, "pass", 8, 5, 3))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    @DisplayName("autenticar: debe lanzar CredencialesInvalidasException cuando la contraseña es incorrecta")
    void autenticar_conPasswordIncorrecta_lanzaExcepcion() {
        // ARRANGE
        String correo = "coordinador@udea.edu.co";
        Coordinador coordinador = Coordinador.builder()
                .id(1L)
                .correo(correo)
                .passwordHash("hash-mockeado-de-prueba")
                .activo(true)
                .build();

        when(coordinadorRepositoryPort.buscarPorCorreo(correo)).thenReturn(Optional.of(coordinador));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.autenticar(correo, "wrongPassword", 8, 5, 3))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    @DisplayName("autenticar: debe lanzar CredencialesInvalidasException cuando el coordinador está inactivo")
    void autenticar_conCoordinadorInactivo_lanzaExcepcion() {
        // ARRANGE
        String correo = "coordinador@udea.edu.co";
        Coordinador coordinadorInactivo = Coordinador.builder()
                .id(1L)
                .correo(correo)
                .passwordHash("hash-mockeado-de-prueba")
                .activo(false)
                .build();

        when(coordinadorRepositoryPort.buscarPorCorreo(correo)).thenReturn(Optional.of(coordinadorInactivo));

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.autenticar(correo, "clave-de-prueba-no-real", 8, 5, 3))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}
