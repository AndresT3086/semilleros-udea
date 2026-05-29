package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PestanaOrganizacionResponse {
    private List<FiltroItemResponse> recursosSeleccionados;
    private List<FiltroItemResponse> fuentesSeleccionadas;
    private List<FiltroItemResponse> todosLosRecursos;
    private List<FiltroItemResponse> todasLasFuentes;
}
