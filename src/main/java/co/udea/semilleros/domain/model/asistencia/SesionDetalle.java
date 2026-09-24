package co.udea.semilleros.domain.model.asistencia;

import java.util.List;

/**
 * Sesión con la lista de asistencia de cada integrante.
 */
public record SesionDetalle(Sesion sesion, List<Asistente> asistencias) {

    public record Asistente(Long idIntegrante, String nombre, String cedula, EstadoAsistencia estado) {
    }
}
