package co.udea.semilleros.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GuardarPestanaProduccionRequest {

    // ¿Tienen artículos científicos?
    private Boolean tienenArticulos;

    @Min(value = 0, message = "La cantidad de artículos no puede ser negativa")
    private Integer cantidadArticulos;

    // ¿Tienen libros o capítulos?
    private Boolean tienenLibros;

    @Min(value = 0, message = "La cantidad de libros no puede ser negativa")
    private Integer cantidadLibros;

    // ¿Organizan eventos?
    private Boolean organizanEventos;

    @Min(value = 0, message = "La cantidad de eventos no puede ser negativa")
    private Integer cantidadEventosOrganizados;

    // ¿Participan en eventos?
    private Boolean participaEnEventos;

    @Min(value = 0, message = "La cantidad de participaciones no puede ser negativa")
    private Integer cantidadParticipaciones;
}
