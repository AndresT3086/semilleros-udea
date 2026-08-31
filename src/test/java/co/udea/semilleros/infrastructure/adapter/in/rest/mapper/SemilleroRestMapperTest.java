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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SemilleroRestMapper - Pruebas unitarias")
class SemilleroRestMapperTest {

    private final SemilleroRestMapper mapper = new SemilleroRestMapperImpl();

    // ─── toResumenResponse ──────────────────────────────────────────────────────

    @Test
    @DisplayName("toResumenResponse: debe mapear los campos del semillero, incluyendo facultad/campus/estado")
    void toResumenResponse_conSemillero_mapeaCampos() {
        // ARRANGE
        Semillero semillero = Semillero.builder()
                .id(1L).codigo("SEM-UDEA-0001").nombre("Semillero IA").siglas("SIA")
                .anioCreacion(2020).grupoInvestigacion("GrupoX")
                .estado(Semillero.EstadoSemillero.ACTIVO)
                .nombreUnidad("Facultad de Ingeniería").nombreCampus("Ciudad Universitaria")
                .totalSemilleristas(5).totalActividadesCientificas(2)
                .build();

        // ACT
        SemilleroResumenResponse resultado = mapper.toResumenResponse(semillero);

        // ASSERT
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getFacultad()).isEqualTo("Facultad de Ingeniería");
        assertThat(resultado.getCampus()).isEqualTo("Ciudad Universitaria");
        assertThat(resultado.getEstado()).isEqualTo("ACTIVO");
        assertThat(resultado.getTotalSemilleristas()).isEqualTo(5);
    }

    @Test
    @DisplayName("toResumenResponse: debe retornar null cuando el semillero es null")
    void toResumenResponse_conSemilleroNulo_retornaNull() {
        assertThat(mapper.toResumenResponse(null)).isNull();
    }

    @Test
    @DisplayName("toResumenResponse: debe mapear estado null cuando el semillero no tiene estado")
    void toResumenResponse_sinEstado_mapeaEstadoNull() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).build();

        // ACT
        SemilleroResumenResponse resultado = mapper.toResumenResponse(semillero);

        // ASSERT
        assertThat(resultado.getEstado()).isNull();
    }

    // ─── toDetalleResponse ──────────────────────────────────────────────────────

    @Test
    @DisplayName("toDetalleResponse: debe mapear todos los campos del semillero")
    void toDetalleResponse_conSemillero_mapeaCampos() {
        // ARRANGE
        LocalDateTime ahora = LocalDateTime.now();
        Semillero semillero = Semillero.builder()
                .id(1L).codigo("SEM-UDEA-0001").nombre("Semillero IA").siglas("SIA")
                .correoSemillero("ia@udea.edu.co").telefono("3000000000").anioCreacion(2020)
                .mision("M").vision("V").objetivo("O")
                .lineasInvestigacion("Líneas").palabrasClave("IA, ML")
                .grupoInvestigacion("GrupoX")
                .estado(Semillero.EstadoSemillero.CARACTERIZADO)
                .estadoCaracterizacion("COMPLETO")
                .fechaCreacion(ahora).fechaActualizacion(ahora)
                .nombreUnidad("Facultad de Ingeniería").nombreCampus("Ciudad Universitaria")
                .nombreAreaOcde("Ciencias Naturales")
                .totalSemilleristas(5).totalActividadesCientificas(2)
                .build();

        // ACT
        SemilleroDetalleResponse resultado = mapper.toDetalleResponse(semillero);

        // ASSERT
        assertThat(resultado.getNombre()).isEqualTo("Semillero IA");
        assertThat(resultado.getFacultad()).isEqualTo("Facultad de Ingeniería");
        assertThat(resultado.getCampus()).isEqualTo("Ciudad Universitaria");
        assertThat(resultado.getAreaOcde()).isEqualTo("Ciencias Naturales");
        assertThat(resultado.getEstado()).isEqualTo("CARACTERIZADO");
        assertThat(resultado.getEstadoCaracterizacion()).isEqualTo("COMPLETO");
        assertThat(resultado.getFechaCreacion()).isEqualTo(ahora);
    }

    @Test
    @DisplayName("toDetalleResponse: debe retornar null cuando el semillero es null")
    void toDetalleResponse_conSemilleroNulo_retornaNull() {
        assertThat(mapper.toDetalleResponse(null)).isNull();
    }

    // ─── toInscripcionResponse / toInscripcionDomain ───────────────────────────

    @Test
    @DisplayName("toInscripcionResponse: debe mapear la inscripción incluyendo el estado como texto")
    void toInscripcionResponse_conInscripcion_mapeaCampos() {
        // ARRANGE
        LocalDateTime ahora = LocalDateTime.now();
        Inscripcion inscripcion = Inscripcion.builder()
                .id(1L).idSemillero(2L).nombreSemillero("Semillero IA")
                .nombres("Juan").apellidos("Pérez").correo("juan@udea.edu.co")
                .estado(Inscripcion.EstadoInscripcion.PENDIENTE)
                .fechaInscripcion(ahora)
                .build();

        // ACT
        InscripcionResponse resultado = mapper.toInscripcionResponse(inscripcion);

        // ASSERT
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo("PENDIENTE");
        assertThat(resultado.getFechaInscripcion()).isEqualTo(ahora);
    }

    @Test
    @DisplayName("toInscripcionResponse: debe retornar null cuando la inscripción es null")
    void toInscripcionResponse_conInscripcionNula_retornaNull() {
        assertThat(mapper.toInscripcionResponse(null)).isNull();
    }

    @Test
    @DisplayName("toInscripcionDomain: debe mapear el request al modelo de dominio")
    void toInscripcionDomain_conRequest_mapeaCampos() {
        // ARRANGE
        InscripcionRequest request = new InscripcionRequest();
        request.setIdSemillero(1L);
        request.setNombres("Juan");
        request.setApellidos("Pérez");
        request.setCedula("123");
        request.setCorreo("juan@udea.edu.co");
        request.setTelefono("3000000000");
        request.setPrograma("Ingeniería");
        request.setSemestre("5");
        request.setMotivacion("Motivación");
        request.setAceptaTerminos(true);

        // ACT
        Inscripcion resultado = mapper.toInscripcionDomain(request);

        // ASSERT
        assertThat(resultado.getIdSemillero()).isEqualTo(1L);
        assertThat(resultado.getCorreo()).isEqualTo("juan@udea.edu.co");
        assertThat(resultado.getAceptaTerminos()).isTrue();
    }

    @Test
    @DisplayName("toInscripcionDomain: debe retornar null cuando el request es null")
    void toInscripcionDomain_conRequestNulo_retornaNull() {
        assertThat(mapper.toInscripcionDomain(null)).isNull();
    }

    // ─── toPestanaGeneralDomain ─────────────────────────────────────────────────

    @Test
    @DisplayName("toPestanaGeneralDomain: debe mapear el request de la pestaña General al dominio")
    void toPestanaGeneralDomain_conRequest_mapeaCampos() {
        // ARRANGE
        GuardarPestanaGeneralRequest request = new GuardarPestanaGeneralRequest();
        request.setNombre("Semillero IA");
        request.setSiglas("SIA");
        request.setCorreoSemillero("ia@udea.edu.co");
        request.setTelefono("3000000000");
        request.setAnioCreacion(2020);
        request.setMision("M");
        request.setVision("V");
        request.setObjetivo("O");
        request.setLineasInvestigacion("Líneas");
        request.setPalabrasClave("IA, ML");
        request.setGrupoInvestigacion("GrupoX");
        request.setIdUnidadAcademica(1L);
        request.setIdCampus(2L);
        request.setIdAreaOcde(3L);

        // ACT
        Semillero resultado = mapper.toPestanaGeneralDomain(request);

        // ASSERT
        assertThat(resultado.getNombre()).isEqualTo("Semillero IA");
        assertThat(resultado.getIdUnidadAcademica()).isEqualTo(1L);
        assertThat(resultado.getIdCampus()).isEqualTo(2L);
        assertThat(resultado.getIdAreaOcde()).isEqualTo(3L);
    }

    @Test
    @DisplayName("toPestanaGeneralDomain: debe retornar null cuando el request es null")
    void toPestanaGeneralDomain_conRequestNulo_retornaNull() {
        assertThat(mapper.toPestanaGeneralDomain(null)).isNull();
    }

    // ─── toPageResponse (default) ───────────────────────────────────────────────

    @Test
    @DisplayName("toPageResponse: debe mapear el contenido y los metadatos de paginación")
    void toPageResponse_conContenido_mapeaPaginacion() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).nombre("Semillero IA").build();
        PageResult<Semillero> pageResult = PageResult.<Semillero>builder()
                .contenido(List.of(semillero))
                .paginaActual(0).tamano(15).totalElementos(1L).totalPaginas(1)
                .esUltimaPagina(true).esPrimeraPagina(true)
                .build();

        // ACT
        PageResponse<SemilleroResumenResponse> resultado = mapper.toPageResponse(pageResult);

        // ASSERT
        assertThat(resultado.getContenido()).hasSize(1);
        assertThat(resultado.getContenido().get(0).getNombre()).isEqualTo("Semillero IA");
        assertThat(resultado.getTotalElementos()).isEqualTo(1L);
        assertThat(resultado.isEsUltimaPagina()).isTrue();
    }

    // ─── toFiltroItemResponse / campusToFiltroItemResponse / areaToFiltroItemResponse (default) ─

    @Test
    @DisplayName("toFiltroItemResponse: debe mapear una unidad académica")
    void toFiltroItemResponse_conUnidad_mapeaCampos() {
        // ARRANGE
        UnidadAcademica unidad = UnidadAcademica.builder().id(1L).nombre("Facultad").siglas("FI").build();

        // ACT
        FiltroItemResponse resultado = mapper.toFiltroItemResponse(unidad);

        // ASSERT
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Facultad");
        assertThat(resultado.getSiglas()).isEqualTo("FI");
    }

    @Test
    @DisplayName("campusToFiltroItemResponse: debe mapear un campus")
    void campusToFiltroItemResponse_conCampus_mapeaCampos() {
        // ARRANGE
        Campus campus = Campus.builder().id(1L).nombre("Ciudad Universitaria").build();

        // ACT
        FiltroItemResponse resultado = mapper.campusToFiltroItemResponse(campus);

        // ASSERT
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Ciudad Universitaria");
    }

    @Test
    @DisplayName("areaToFiltroItemResponse: debe mapear un área OCDE")
    void areaToFiltroItemResponse_conArea_mapeaCampos() {
        // ARRANGE
        AreaOcde area = AreaOcde.builder().id(1L).nombre("Ciencias Naturales").build();

        // ACT
        FiltroItemResponse resultado = mapper.areaToFiltroItemResponse(area);

        // ASSERT
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Ciencias Naturales");
    }
}
