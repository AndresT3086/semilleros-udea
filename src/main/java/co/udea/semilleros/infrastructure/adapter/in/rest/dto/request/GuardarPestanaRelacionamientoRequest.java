package co.udea.semilleros.infrastructure.adapter.in.rest.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GuardarPestanaRelacionamientoRequest {

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
