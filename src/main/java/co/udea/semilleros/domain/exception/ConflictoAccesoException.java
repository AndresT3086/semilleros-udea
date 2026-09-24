package co.udea.semilleros.domain.exception;

public class ConflictoAccesoException extends SemillerosException {

    private static final String ERROR_CODE = "CONFLICTO_ACCESO";

    public ConflictoAccesoException(String mensaje) {
        super(ERROR_CODE, mensaje);
    }
}
