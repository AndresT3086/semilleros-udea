package co.udea.semilleros.infrastructure.security.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de peticiones por IP para los endpoints públicos sensibles.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    record Limite(int peticiones, Duration periodo, String codigo, String mensaje) {
    }

    static final Map<String, Limite> LIMITES = Map.of(
            // Máximo 10 intentos de login por minuto por IP
            "/api/v1/auth/login", new Limite(10, Duration.ofMinutes(1), "DEMASIADOS_INTENTOS",
                    "Demasiados intentos de login. Espere 1 minuto antes de intentar de nuevo."),
            // Registro de coordinadores: evita el envío masivo de solicitudes desde una misma IP
            "/api/v1/solicitudes-acceso", new Limite(3, Duration.ofHours(1), "DEMASIADAS_SOLICITUDES",
                    "Se enviaron demasiadas solicitudes desde esta conexión. Intente de nuevo en una hora."),
            "/api/v1/solicitudes-acceso/verificar", new Limite(10, Duration.ofMinutes(15), "DEMASIADOS_INTENTOS",
                    "Demasiados intentos. Espere unos minutos antes de intentar de nuevo."),
            "/api/v1/cuenta/activar", new Limite(10, Duration.ofMinutes(15), "DEMASIADOS_INTENTOS",
                    "Demasiados intentos. Espere unos minutos antes de intentar de nuevo.")
    );

    // Un bucket por endpoint e IP
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String ruta = request.getRequestURI();
        Limite limite = LIMITES.get(ruta);
        if (limite == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = obtenerIpReal(request);
        Bucket bucket = buckets.computeIfAbsent(ruta + "|" + ip, clave -> crearBucket(limite));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                {
                  "exitoso": false,
                  "codigoError": "%s",
                  "mensaje": "%s"
                }
                """.formatted(limite.codigo(), limite.mensaje()));
        }
    }

    private static Bucket crearBucket(Limite limite) {
        Bandwidth ancho = Bandwidth.classic(limite.peticiones(),
                Refill.greedy(limite.peticiones(), limite.periodo()));
        return Bucket.builder().addLimit(ancho).build();
    }

    static String obtenerIpReal(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
