package co.udea.semilleros.domain.model.asistencia;

public enum EstadoAsistencia {
    PRESENTE,
    AUSENTE,
    /** Ausencia justificada: se descuenta del total esperado y no afecta el porcentaje. */
    EXCUSADO
}
