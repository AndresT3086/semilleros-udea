package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SemilleroDetalleResponse {
    private Long id;
    private String codigo;
    private String nombre;
    private String siglas;
    private String correoSemillero;
    private String telefono;
    private Integer anioCreacion;
    private String mision;
    private String vision;
    private String objetivo;
    private String lineasInvestigacion;
    private String palabrasClave;
    private String grupoInvestigacion;
    private String facultad;
    private String campus;
    private String areaOcde;
    private String estado;
    private Integer totalSemilleristas;
    private Integer totalActividadesCientificas;
    private String estadoCaracterizacion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
