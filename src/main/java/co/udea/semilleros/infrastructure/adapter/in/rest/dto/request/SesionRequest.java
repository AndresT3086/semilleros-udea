package co.udea.semilleros.infrastructure.adapter.in.rest.dto.request;

import co.udea.semilleros.domain.model.asistencia.DatosSesion;
import co.udea.semilleros.domain.model.asistencia.EstadoAsistencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Sesión (actividad) de un semillero con la asistencia de sus integrantes.
 */
@Getter
@Setter
@NoArgsConstructor
public class SesionRequest {

    @NotBlank(message = "El título de la actividad es obligatorio")
    @Size(max = 200, message = "El título no puede superar los 200 caracteres")
    private String titulo;

    @NotNull(message = "La fecha de la actividad es obligatoria")
    private LocalDate fecha;

    /** Tipo de actividad del catálogo (talleres, seminarios...). Opcional. */
    private Long idActividad;

    @NotNull(message = "La lista de asistencia es obligatoria")
    private List<@Valid Asistencia> asistencias = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Asistencia {
        @NotNull(message = "El integrante es obligatorio")
        private Long idIntegrante;

        @NotNull(message = "El estado de asistencia es obligatorio (PRESENTE, AUSENTE o EXCUSADO)")
        private EstadoAsistencia estado;
    }

    public DatosSesion toDatos() {
        return new DatosSesion(titulo, fecha, idActividad, asistencias.stream()
                .map(a -> new DatosSesion.Registro(a.getIdIntegrante(), a.getEstado()))
                .toList());
    }
}
