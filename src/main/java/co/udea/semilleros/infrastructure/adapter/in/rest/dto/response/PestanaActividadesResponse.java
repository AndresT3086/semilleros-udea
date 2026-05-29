package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PestanaActividadesResponse {

    private List<ActividadItemResponse> actividades;

    @Getter
    @Builder
    public static class ActividadItemResponse {
        private Long    idActividad;
        private String  nombre;
        private String  categoria;
        private Boolean realiza;
    }
}
