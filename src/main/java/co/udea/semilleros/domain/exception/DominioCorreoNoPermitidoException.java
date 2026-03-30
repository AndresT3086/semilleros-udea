package co.udea.semilleros.domain.exception;

public class DominioCorreoNoPermitidoException extends SemillerosException {

    private static final String ERROR_CODE = "DOMINIO_CORREO_NO_PERMITIDO";

    public DominioCorreoNoPermitidoException(String correo) {
        super(ERROR_CODE,
                String.format("El correo '%s' no pertenece al dominio institucional permitido (@udea.edu.co).", correo));
    }
}
