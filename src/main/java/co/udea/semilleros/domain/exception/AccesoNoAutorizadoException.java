package co.udea.semilleros.domain.exception;

public class AccesoNoAutorizadoException extends SemillerosException {

    private static final String ERROR_CODE = "ACCESO_NO_AUTORIZADO";

    public AccesoNoAutorizadoException(String recurso) {
        super(ERROR_CODE,
                String.format("No tiene autorización para acceder al recurso: %s.", recurso));
    }
}
