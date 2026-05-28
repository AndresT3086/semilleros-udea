package co.udea.semilleros.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GuardarPestanaActividadesRequest {

    @NotNull(message = "La lista de actividades es obligatoria")
    private List<ActividadItem> actividades;

    @Getter @Setter @NoArgsConstructor
    public static class ActividadItem {
        private Long idActividad;
        private Boolean realiza;
    }
}
