package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.model.SemilleroFiltro;
import co.udea.semilleros.domain.port.in.ConsultarSemillerosUseCase;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.PageResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.SemilleroDetalleResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.SemilleroResumenResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.mapper.SemilleroRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/semilleros")
@RequiredArgsConstructor
@Tag(name = "Semilleros", description = "Endpoints públicos para consulta de semilleros de investigación")
public class SemilleroController {

    private final ConsultarSemillerosUseCase consultarSemillerosUseCase;
    private final SemilleroRestMapper semilleroRestMapper;

    @GetMapping
    @Operation(
        summary = "Listar semilleros activos",
        description = "Retorna una lista paginada de semilleros activos. Máximo 15 por página. "
                    + "Soporta filtros por unidad académica, campus, área OCDE y palabra clave."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Listado obtenido exitosamente",
            content = @Content(schema = @Schema(implementation = PageResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<PageResponse<SemilleroResumenResponse>>> listarSemilleros(
            @Parameter(description = "Número de página (inicia en 0)") @RequestParam(defaultValue = "0") int pagina,
            @Parameter(description = "Tamaño de página (máximo 15)")   @RequestParam(defaultValue = "15") int tamano,
            @Parameter(description = "ID de la unidad académica")      @RequestParam(required = false) Long idUnidad,
            @Parameter(description = "ID del campus")                  @RequestParam(required = false) Long idCampus,
            @Parameter(description = "ID del área OCDE")               @RequestParam(required = false) Long idArea,
            @Parameter(description = "Palabra clave de búsqueda")      @RequestParam(required = false) String q
    ) {
        int tamanoSeguro = Math.min(tamano, 15);

        SemilleroFiltro filtro = SemilleroFiltro.builder()
                .pagina(pagina)
                .tamano(tamanoSeguro)
                .idUnidadAcademica(idUnidad)
                .idCampus(idCampus)
                .idAreaOcde(idArea)
                .palabraClave(q)
                .build();

        PageResult<Semillero> resultado = consultarSemillerosUseCase.listarSemillerosActivos(filtro);
        PageResponse<SemilleroResumenResponse> response = semilleroRestMapper.toPageResponse(resultado);

        return ResponseEntity.ok(ApiResponse.exito(response));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Obtener detalle de un semillero",
        description = "Retorna la información completa de un semillero por su ID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Semillero encontrado",
            content = @Content(schema = @Schema(implementation = SemilleroDetalleResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Semillero no encontrado")
    })
    public ResponseEntity<ApiResponse<SemilleroDetalleResponse>> obtenerDetalle(
            @Parameter(description = "ID del semillero", required = true) @PathVariable Long id
    ) {
        Semillero semillero = consultarSemillerosUseCase.obtenerDetalleSemillero(id);
        SemilleroDetalleResponse response = semilleroRestMapper.toDetalleResponse(semillero);
        return ResponseEntity.ok(ApiResponse.exito(response));
    }
}
