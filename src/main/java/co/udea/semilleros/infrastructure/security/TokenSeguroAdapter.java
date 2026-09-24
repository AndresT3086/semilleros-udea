package co.udea.semilleros.infrastructure.security;

import co.udea.semilleros.domain.model.acceso.TokenGenerado;
import co.udea.semilleros.domain.port.out.TokenSeguroPort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens de 256 bits aleatorios (imposibles de adivinar) codificados para URL.
 * En la base se guarda solo su SHA-256: quien lea la tabla no puede usar los enlaces.
 */
@Component
public class TokenSeguroAdapter implements TokenSeguroPort {

    private static final int BYTES_TOKEN = 32;
    private final SecureRandom aleatorio = new SecureRandom();

    @Override
    public TokenGenerado generar() {
        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new TokenGenerado(valor, hash(valor));
    }

    @Override
    public String hash(String valor) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en la JVM", e);
        }
    }
}
