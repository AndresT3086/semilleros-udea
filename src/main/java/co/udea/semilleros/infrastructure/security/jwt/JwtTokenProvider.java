package co.udea.semilleros.infrastructure.security.jwt;

import co.udea.semilleros.domain.exception.TokenInvalidoException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    static final String CLAIM_ID_USUARIO = "idUsuario";
    static final String CLAIM_ID_ANTERIOR = "idCoordinador";

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(Long idUsuario, String correo, String rol) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(correo)
                .claim(CLAIM_ID_USUARIO, idUsuario)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(secretKey)
                .compact();
    }

    public String extraerCorreo(String token) {
        return parsearClaims(token).getSubject();
    }

    public Long extraerIdUsuario(String token) {
        Claims claims = parsearClaims(token);
        Long idUsuario = claims.get(CLAIM_ID_USUARIO, Long.class);
        // Tokens emitidos antes del cambio de nombre (vigentes hasta su expiración)
        return idUsuario != null ? idUsuario : claims.get(CLAIM_ID_ANTERIOR, Long.class);
    }

    public String extraerRol(String token) {
        return parsearClaims(token).get("rol", String.class);
    }

    public boolean esTokenValido(String token) {
        try {
            parsearClaims(token);
            return true;
        } catch (TokenInvalidoException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    private Claims parsearClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new TokenInvalidoException("el token ha expirado");
        } catch (JwtException e) {
            throw new TokenInvalidoException("firma o estructura inválida");
        }
    }
}
