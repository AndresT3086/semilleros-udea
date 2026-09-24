package co.udea.semilleros.infrastructure.adapter.in.rest.dto.request;

import co.udea.semilleros.domain.model.reporte.ReporteFiltro;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Filtros de reportes recibidos como query params (RN5).
 */
@Getter
@Setter
@NoArgsConstructor
public class ReporteFiltroRequest {

    @Schema(description = "Año académico (2025) o semestre (2025-1, 2025-2). Vacío = estado actual", example = "2025-1")
    private String periodo;

    @Schema(description = "FACULTAD, ESCUELA, INSTITUTO, CORPORACION o SECCIONAL. Vacío = todas", example = "FACULTAD")
    private String tipoUnidad;

    @Schema(description = "Id de la unidad académica")
    private Long idUnidad;

    @Schema(description = "Id del campus o seccional")
    private Long idCampus;

    @Schema(description = "Id de un semillero específico")
    private Long idSemillero;

    public ReporteFiltro toFiltro() {
        return ReporteFiltro.de(periodo, tipoUnidad, idUnidad, idCampus, idSemillero);
    }
}
