package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PestanaRelacionamientoResponse {

    private Boolean adscritoGrupo;
    private String  grupoInvestigacion;
    private String  relacionGrupo;
    private String  centroInvestigaciones;
    private String  relacionCentro;
    private String  departamento;
    private String  relacionDepartamento;
    private String  facultad;
    private String  relacionFacultad;
}
