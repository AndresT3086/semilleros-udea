package co.udea.semilleros.domain.exception;

public class DatosAsistenciaInvalidosException extends SemillerosException {

    private static final String ERROR_CODE = "DATOS_ASISTENCIA_INVALIDOS";

    public DatosAsistenciaInvalidosException(String mensaje) {
        super(ERROR_CODE, mensaje);
    }
}
