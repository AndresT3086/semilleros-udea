package co.udea.semilleros.domain.exception;

public class SolicitudAccesoInvalidaException extends SemillerosException {

    private static final String ERROR_CODE = "SOLICITUD_ACCESO_INVALIDA";

    public SolicitudAccesoInvalidaException(String mensaje) {
        super(ERROR_CODE, mensaje);
    }
}
