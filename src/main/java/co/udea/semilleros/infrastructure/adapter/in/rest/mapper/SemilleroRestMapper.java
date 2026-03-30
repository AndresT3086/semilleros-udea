package co.udea.semilleros.infrastructure.adapter.in.rest.mapper;

import co.udea.semilleros.domain.model.AreaOcde;
import co.udea.semilleros.domain.model.Campus;
import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.model.UnidadAcademica;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.request.GuardarPestanaGeneralRequest;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.request.InscripcionRequest;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.FiltroItemResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.InscripcionResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.PageResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.SemilleroDetalleResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.SemilleroResumenResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SemilleroRestMapper {

    @Mapping(target = "facultad", source = "idUnidadAcademica", ignore = true)
    @Mapping(target = "campus", source = "idCampus", ignore = true)
    @Mapping(target = "estado", expression = "java(semillero.getEstado() != null ? semillero.getEstado().name() : null)")
    SemilleroResumenResponse toResumenResponse(Semillero semillero);

    @Mapping(target = "facultad", ignore = true)
    @Mapping(target = "campus", ignore = true)
    @Mapping(target = "areaOcde", ignore = true)
    @Mapping(target = "estado", expression = "java(semillero.getEstado() != null ? semillero.getEstado().name() : null)")
    SemilleroDetalleResponse toDetalleResponse(Semillero semillero);

    @Mapping(target = "estado", expression = "java(inscripcion.getEstado() != null ? inscripcion.getEstado().name() : null)")
    InscripcionResponse toInscripcionResponse(Inscripcion inscripcion);

    @Mapping(target = "idSemillero", source = "idSemillero")
    Inscripcion toInscripcionDomain(InscripcionRequest request);

    Semillero toPestanaGeneralDomain(GuardarPestanaGeneralRequest request);

    default PageResponse<SemilleroResumenResponse> toPageResponse(PageResult<Semillero> pageResult) {
        List<SemilleroResumenResponse> contenido = pageResult.getContenido().stream()
                .map(this::toResumenResponse)
                .toList();
        return PageResponse.<SemilleroResumenResponse>builder()
                .contenido(contenido)
                .paginaActual(pageResult.getPaginaActual())
                .tamano(pageResult.getTamano())
                .totalElementos(pageResult.getTotalElementos())
                .totalPaginas(pageResult.getTotalPaginas())
                .esUltimaPagina(pageResult.isEsUltimaPagina())
                .esPrimeraPagina(pageResult.isEsPrimeraPagina())
                .build();
    }

    default FiltroItemResponse toFiltroItemResponse(UnidadAcademica unidad) {
        return FiltroItemResponse.builder()
                .id(unidad.getId())
                .nombre(unidad.getNombre())
                .siglas(unidad.getSiglas())
                .build();
    }

    default FiltroItemResponse campusToFiltroItemResponse(Campus campus) {
        return FiltroItemResponse.builder()
                .id(campus.getId())
                .nombre(campus.getNombre())
                .build();
    }

    default FiltroItemResponse areaToFiltroItemResponse(AreaOcde area) {
        return FiltroItemResponse.builder()
                .id(area.getId())
                .nombre(area.getNombre())
                .build();
    }
}
