package co.udea.semilleros.domain.exception;

public class InscripcionDuplicadaException extends SemillerosException {

    private static final String ERROR_CODE = "INSCRIPCION_DUPLICADA";

    public InscripcionDuplicadaException(String correo, Long idSemillero) {
        super(ERROR_CODE,
                String.format("El estudiante con correo '%s' ya tiene una inscripción pendiente o aprobada en el semillero %d.",
                        correo, idSemillero));
    }
}
