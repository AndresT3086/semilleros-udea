package co.udea.semilleros.domain.exception;

public class ValidacionBotException extends SemillerosException {

    private static final String ERROR_CODE = "VALIDACION_BOT_FALLIDA";

    public ValidacionBotException() {
        super(ERROR_CODE, "La validación matemática anti-bot no fue resuelta correctamente.");
    }
}
