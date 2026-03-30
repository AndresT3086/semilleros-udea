package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SemilleroResumenResponse {
    private Long id;
    private String codigo;
    private String nombre;
    private String siglas;
    private String facultad;
    private String campus;
    private Integer anioCreacion;
    private String grupoInvestigacion;
    private Integer totalSemilleristas;
    private Integer totalActividadesCientificas;
    private String estado;
}
