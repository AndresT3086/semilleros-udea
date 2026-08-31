package co.udea.semilleros.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter - Pruebas unitarias")
class RateLimitFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
    }

    // ─── doFilterInternal - endpoints no limitados ─────────────────────────────

    @Test
    @DisplayName("doFilterInternal: debe dejar pasar la cadena sin limitar cuando la URI no es /api/v1/auth/login")
    void doFilterInternal_conUriNoLogin_dejaPasarSinLimitar() throws Exception {
        // ARRANGE
        when(request.getRequestURI()).thenReturn("/api/v1/semilleros");

        // ACT
        filter.doFilterInternal(request, response, filterChain);

        // ASSERT
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyIntSafely());
    }

    // ─── doFilterInternal - límite de intentos de login ────────────────────────

    @Test
    @DisplayName("doFilterInternal: debe permitir hasta 10 intentos de login por IP y bloquear el número 11")
    void doFilterInternal_conMasDe10Intentos_bloqueaConDemasiadosIntentos() throws Exception {
        // ARRANGE
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        // ACT: 10 intentos permitidos
        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        // ASSERT: la cadena se invocó las 10 veces, sin bloqueo aún
        verify(filterChain, times(10)).doFilter(request, response);
        verify(response, never()).setStatus(429);

        // ACT: intento 11, debe bloquear
        filter.doFilterInternal(request, response, filterChain);

        // ASSERT: la cadena NO se invoca una undécima vez, se responde 429
        verify(filterChain, times(10)).doFilter(request, response);
        verify(response).setStatus(429);
        verify(response).setContentType("application/json;charset=UTF-8");
        assertThat(body.toString()).contains("DEMASIADOS_INTENTOS");
    }

    @Test
    @DisplayName("doFilterInternal: debe usar buckets independientes por IP (X-Forwarded-For)")
    void doFilterInternal_conIpsDiferentes_usaBucketsIndependientes() throws Exception {
        // ARRANGE
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");

        // ACT
        filter.doFilterInternal(request, response, filterChain);

        // ASSERT: se consumió del bucket de la IP "203.0.113.5" (primer valor del header),
        // por lo que no debería haberse tocado getRemoteAddr como fallback.
        verify(request, never()).getRemoteAddr();
        verify(filterChain).doFilter(request, response);
    }

    private static int anyIntSafely() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
