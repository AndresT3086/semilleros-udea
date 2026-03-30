package co.udea.semilleros.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDateTime;

@Getter
@Builder
@With
public class Inscripcion {

    private final Long id;
    private final Long idSemillero;
    private final String nombreSemillero;

    // Datos del estudiante
    private final String nombres;
    private final String apellidos;
    private final String cedula;
    private final String correo;
    private final String telefono;
    private final String programa;
    private final String semestre;
    private final String motivacion;
    private final Boolean aceptaTerminos;

    private final EstadoInscripcion estado;
    private final LocalDateTime fechaInscripcion;
    private final LocalDateTime fechaActualizacion;

    public enum EstadoInscripcion {
        PENDIENTE, APROBADO, RECHAZADO
    }
}
