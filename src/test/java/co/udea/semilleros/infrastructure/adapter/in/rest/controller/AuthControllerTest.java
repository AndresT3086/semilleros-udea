package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.exception.CredencialesInvalidasException;
import co.udea.semilleros.domain.port.in.AutenticarCoordinadorUseCase;
import co.udea.semilleros.infrastructure.config.GlobalExceptionHandler;
import co.udea.semilleros.infrastructure.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("AuthController - Pruebas de integración de capa web")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AutenticarCoordinadorUseCase autenticarCoordinadorUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("GET /captcha-math: debe retornar una operación con operandos entre 1 y 19")
    void obtenerCaptchaMath_retornaOperandosEnRango() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/auth/captcha-math"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.operacion").value("SUMA"))
                .andExpect(jsonPath("$.datos.operando1", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.datos.operando1", lessThanOrEqualTo(19)))
                .andExpect(jsonPath("$.datos.operando2", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.datos.operando2", lessThanOrEqualTo(19)));
    }

    @Test
    @DisplayName("POST /login: debe retornar 200 y un token cuando las credenciales son válidas")
    void login_conCredencialesValidas_retorna200() throws Exception {
        // ARRANGE
        String token = "eyJhbGciOiJIUzI1NiJ9.test.token";
        when(autenticarCoordinadorUseCase.autenticar(anyString(), anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(token);
        when(jwtTokenProvider.extraerIdCoordinador(token)).thenReturn(5L);

        String body = """
                {"correo":"coordinador@udea.edu.co","password":"clave-de-prueba-no-real","respuestaMath":7,"operando1":3,"operando2":4}
                """;

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.token").value(token))
                .andExpect(jsonPath("$.datos.idCoordinador").value(5));
    }

    @Test
    @DisplayName("POST /login: debe retornar 401 cuando las credenciales son inválidas")
    void login_conCredencialesInvalidas_retorna401() throws Exception {
        // ARRANGE
        when(autenticarCoordinadorUseCase.autenticar(anyString(), anyString(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new CredencialesInvalidasException());

        String body = """
                {"correo":"coordinador@udea.edu.co","password":"clave-erronea-de-prueba","respuestaMath":7,"operando1":3,"operando2":4}
                """;

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exitoso").value(false));
    }

    @Test
    @DisplayName("POST /login: debe retornar 400 cuando el correo no tiene formato válido")
    void login_conCorreoInvalido_retorna400() throws Exception {
        // ACT & ASSERT
        String body = """
                {"correo":"no-es-un-correo","password":"clave-de-prueba","respuestaMath":7,"operando1":3,"operando2":4}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
