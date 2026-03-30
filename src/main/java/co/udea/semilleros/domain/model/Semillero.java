package co.udea.semilleros.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDateTime;

@Getter
@Builder
@With
public class Semillero {

    private final Long id;
    private final String codigo;
    private final String nombre;
    private final String siglas;
    private final String correoSemillero;
    private final String telefono;
    private final Integer anioCreacion;
    private final String mision;
    private final String vision;
    private final String objetivo;
    private final String lineasInvestigacion;
    private final String palabrasClave;
    private final String grupoInvestigacion;
    private final EstadoSemillero estado;
    private final String estadoCaracterizacion;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaActualizacion;

    private final Long idUnidadAcademica;
    private final Long idCampus;
    private final Long idAreaOcde;
    private final Long idCoordinador;

    // Counts for display
    private final Integer totalSemilleristas;
    private final Integer totalActividadesCientificas;

    public enum EstadoSemillero {
        ACTIVO, INACTIVO, BORRADOR, CARACTERIZADO
    }
}
