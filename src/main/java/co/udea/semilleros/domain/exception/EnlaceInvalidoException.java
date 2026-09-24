package co.udea.semilleros.domain.exception;

/**
 * Enlace de verificación o activación inexistente, vencido o ya usado.
 * El mensaje no distingue el caso para no revelar información.
 */
public class EnlaceInvalidoException extends SemillerosException {

    private static final String ERROR_CODE = "ENLACE_INVALIDO";

    public EnlaceInvalidoException() {
        super(ERROR_CODE, "El enlace no es válido o ya venció. Solicita uno nuevo.");
    }
}
