package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.model.AreaOcde;
import co.udea.semilleros.domain.model.Campus;
import co.udea.semilleros.domain.model.UnidadAcademica;
import co.udea.semilleros.domain.port.in.ConsultarFiltrosUseCase;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.FiltroItemResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.mapper.SemilleroRestMapper;
import co.udea.semilleros.infrastructure.config.GlobalExceptionHandler;
import co.udea.semilleros.infrastructure.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FiltrosController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("FiltrosController - Pruebas de integración de capa web")
class FiltrosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultarFiltrosUseCase consultarFiltrosUseCase;

    @MockBean
    private SemilleroRestMapper semilleroRestMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("GET /unidades-academicas: debe retornar el listado de unidades")
    void listarUnidades_conDatos_retorna200() throws Exception {
        // ARRANGE
        UnidadAcademica unidad = UnidadAcademica.builder().id(1L).nombre("Facultad de Ingeniería").build();
        when(consultarFiltrosUseCase.listarUnidadesAcademicas()).thenReturn(List.of(unidad));
        when(semilleroRestMapper.toFiltroItemResponse(unidad))
                .thenReturn(FiltroItemResponse.builder().id(1L).nombre("Facultad de Ingeniería").build());

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/filtros/unidades-academicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos", hasSize(1)));
    }

    @Test
    @DisplayName("GET /campus: debe retornar lista vacía cuando no hay campus registrados")
    void listarCampus_sinDatos_retorna200ConListaVacia() throws Exception {
        // ARRANGE
        when(consultarFiltrosUseCase.listarCampus()).thenReturn(List.of());

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/filtros/campus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos", hasSize(0)));
    }

    @Test
    @DisplayName("GET /areas-ocde: debe retornar el listado de áreas OCDE")
    void listarAreasOcde_conDatos_retorna200() throws Exception {
        // ARRANGE
        AreaOcde area = AreaOcde.builder().id(1L).nombre("Ciencias Naturales").build();
        when(consultarFiltrosUseCase.listarAreasOcde()).thenReturn(List.of(area));
        when(semilleroRestMapper.areaToFiltroItemResponse(area))
                .thenReturn(FiltroItemResponse.builder().id(1L).nombre("Ciencias Naturales").build());

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/filtros/areas-ocde"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos", hasSize(1)));
    }
}
