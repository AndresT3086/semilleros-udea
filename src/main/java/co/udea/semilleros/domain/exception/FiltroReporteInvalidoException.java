package co.udea.semilleros.domain.exception;

public class FiltroReporteInvalidoException extends SemillerosException {

    private static final String ERROR_CODE = "FILTRO_REPORTE_INVALIDO";

    public FiltroReporteInvalidoException(String mensaje) {
        super(ERROR_CODE, mensaje);
    }
}
