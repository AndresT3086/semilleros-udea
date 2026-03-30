package co.udea.semilleros.domain.exception;

public class CredencialesInvalidasException extends SemillerosException {

    private static final String ERROR_CODE = "CREDENCIALES_INVALIDAS";

    public CredencialesInvalidasException() {
        super(ERROR_CODE, "Las credenciales proporcionadas son inválidas.");
    }
}
