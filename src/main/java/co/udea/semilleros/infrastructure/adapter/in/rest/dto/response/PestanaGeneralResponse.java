package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PestanaGeneralResponse {

    private Long    id;
    private String  codigo;
    private String  nombre;
    private String  siglas;
    private String  correoSemillero;
    private String  telefono;
    private Integer anioCreacion;
    private String  mision;
    private String  vision;
    private String  objetivo;
    private String  lineasInvestigacion;
    private String  palabrasClave;
    private String  grupoInvestigacion;
    private Long    idUnidadAcademica;
    private String  nombreUnidad;
    private Long    idCampus;
    private String  nombreCampus;
    private Long    idAreaOcde;
    private String  nombreAreaOcde;
    private String  estadoCaracterizacion;
}
