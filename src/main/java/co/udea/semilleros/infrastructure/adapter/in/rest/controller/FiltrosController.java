package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.port.in.ConsultarFiltrosUseCase;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.FiltroItemResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.mapper.SemilleroRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/filtros")
@RequiredArgsConstructor
@Tag(name = "Filtros", description = "Endpoints para obtener datos de filtros (unidades, campus, áreas OCDE)")
public class FiltrosController {

    private final ConsultarFiltrosUseCase consultarFiltrosUseCase;
    private final SemilleroRestMapper semilleroRestMapper;

    @GetMapping("/unidades-academicas")
    @Operation(summary = "Listar unidades académicas", description = "Retorna todas las facultades/unidades académicas disponibles para filtrar semilleros.")
    public ResponseEntity<ApiResponse<List<FiltroItemResponse>>> listarUnidades() {
        List<FiltroItemResponse> items = consultarFiltrosUseCase.listarUnidadesAcademicas()
                .stream()
                .map(semilleroRestMapper::toFiltroItemResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.exito(items));
    }

    @GetMapping("/campus")
    @Operation(summary = "Listar campus", description = "Retorna todos los campus disponibles para filtrar semilleros.")
    public ResponseEntity<ApiResponse<List<FiltroItemResponse>>> listarCampus() {
        List<FiltroItemResponse> items = consultarFiltrosUseCase.listarCampus()
                .stream()
                .map(semilleroRestMapper::campusToFiltroItemResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.exito(items));
    }

    @GetMapping("/areas-ocde")
    @Operation(summary = "Listar áreas OCDE", description = "Retorna todas las áreas de conocimiento OCDE disponibles para filtrar semilleros.")
    public ResponseEntity<ApiResponse<List<FiltroItemResponse>>> listarAreasOcde() {
        List<FiltroItemResponse> items = consultarFiltrosUseCase.listarAreasOcde()
                .stream()
                .map(semilleroRestMapper::areaToFiltroItemResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.exito(items));
    }
}
