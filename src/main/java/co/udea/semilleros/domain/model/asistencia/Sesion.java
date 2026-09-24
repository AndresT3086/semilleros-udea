package co.udea.semilleros.domain.model.asistencia;

import java.time.LocalDate;

/**
 * Actividad realizada por un semillero en una fecha, con el resumen de su asistencia.
 */
public record Sesion(
        Long id,
        Long idSemillero,
        Long idActividad,
        String actividad,
        String titulo,
        LocalDate fecha,
        ConteoAsistencia asistencia
) {
}
