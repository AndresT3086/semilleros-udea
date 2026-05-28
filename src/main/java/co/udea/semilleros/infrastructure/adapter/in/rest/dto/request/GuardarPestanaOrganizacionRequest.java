package co.udea.semilleros.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GuardarPestanaOrganizacionRequest {
    @NotEmpty(message = "Debe indicar al menos un recurso o seleccionar Ninguno")
    private List<Long> idsRecursos;

    @NotEmpty(message = "Debe indicar al menos una fuente o seleccionar Sin financiación")
    private List<Long> idsFuentesFinanciacion;
}
