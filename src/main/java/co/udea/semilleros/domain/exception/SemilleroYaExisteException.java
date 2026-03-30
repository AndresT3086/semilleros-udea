package co.udea.semilleros.domain.exception;

public class SemilleroYaExisteException extends SemillerosException {

    private static final String ERROR_CODE = "SEMILLERO_YA_EXISTE";

    public SemilleroYaExisteException(String campo, String valor) {
        super(ERROR_CODE,
                String.format("Ya existe un semillero con %s: '%s'.", campo, valor));
    }
}
