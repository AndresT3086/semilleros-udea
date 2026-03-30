package co.udea.semilleros.domain.exception;

public class TokenInvalidoException extends SemillerosException {

    private static final String ERROR_CODE = "TOKEN_INVALIDO";

    public TokenInvalidoException(String motivo) {
        super(ERROR_CODE, String.format("El token JWT es inválido: %s.", motivo));
    }
}
