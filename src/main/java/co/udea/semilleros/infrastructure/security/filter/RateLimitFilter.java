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

@Component
public class RateLimitFilter  extends OncePerRequestFilter {

    // Un bucket por IP — máximo 10 intentos de login por minuto por IP
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Solo limitar el endpoint de login
        if (!request.getRequestURI().equals("/api/v1/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = obtenerIpReal(request);
        Bucket bucket = buckets.computeIfAbsent(ip, this::crearBucket);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                {
                  "exitoso": false,
                  "codigoError": "DEMASIADOS_INTENTOS",
                  "mensaje": "Demasiados intentos de login. Espere 1 minuto antes de intentar de nuevo."
                }
                """);
        }
    }

    private Bucket crearBucket(String ip) {
        // 10 tokens, recarga 10 por minuto
        Bandwidth limite = Bandwidth.classic(10,
                Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limite).build();
    }

    private String obtenerIpReal(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
