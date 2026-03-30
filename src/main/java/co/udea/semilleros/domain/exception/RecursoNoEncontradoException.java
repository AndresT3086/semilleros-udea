package co.udea.semilleros.domain.exception;

public class RecursoNoEncontradoException extends SemillerosException {

    private static final String ERROR_CODE = "RECURSO_NO_ENCONTRADO";

    public RecursoNoEncontradoException(String recurso, Long id) {
        super(ERROR_CODE, String.format("%s con id %d no fue encontrado.", recurso, id));
    }

    public RecursoNoEncontradoException(String recurso, String identificador) {
        super(ERROR_CODE, String.format("%s '%s' no fue encontrado.", recurso, identificador));
    }
}
