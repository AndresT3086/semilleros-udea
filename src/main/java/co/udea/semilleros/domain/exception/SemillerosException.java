package co.udea.semilleros.domain.exception;

public abstract class SemillerosException extends RuntimeException {

    private final String errorCode;

    protected SemillerosException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected SemillerosException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
