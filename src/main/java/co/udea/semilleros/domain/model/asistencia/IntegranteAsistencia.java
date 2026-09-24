package co.udea.semilleros.domain.model.asistencia;

/**
 * Integrante de un semillero con su asistencia acumulada en el período consultado.
 */
public record IntegranteAsistencia(Long idIntegrante, String nombre, String cedula, boolean activo,
                                   ConteoAsistencia asistencia) {
}
