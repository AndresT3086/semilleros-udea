package co.udea.semilleros.domain.model.asistencia;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos que envía el coordinador para registrar o corregir una sesión y su asistencia.
 */
public record DatosSesion(String titulo, LocalDate fecha, Long idActividad, List<Registro> asistencias) {

    public record Registro(Long idIntegrante, EstadoAsistencia estado) {
    }
}
