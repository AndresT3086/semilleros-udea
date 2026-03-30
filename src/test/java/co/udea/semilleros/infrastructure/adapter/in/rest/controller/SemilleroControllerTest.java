package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.exception.RecursoNoEncontradoException;
import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.port.in.ConsultarSemillerosUseCase;
import co.udea.semilleros.infrastructure.adapter.in.rest.mapper.SemilleroRestMapper;
import co.udea.semilleros.infrastructure.config.GlobalExceptionHandler;
import co.udea.semilleros.infrastructure.config.SecurityConfig;
import co.udea.semilleros.infrastructure.security.filter.JwtAuthenticationFilter;
import co.udea.semilleros.infrastructure.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SemilleroController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@DisplayName("SemilleroController - Pruebas de integración de capa web")
class SemilleroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultarSemillerosUseCase consultarSemillerosUseCase;

    @MockBean
    private SemilleroRestMapper semilleroRestMapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    // ─── GET /api/v1/semilleros ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/semilleros: debe retornar 200 con lista paginada de semilleros")
    void listarSemilleros_retorna200ConPaginaDeResultados() throws Exception {
        // ARRANGE
        Semillero semillero = Semillero.builder()
                .id(1L)
                .nombre("Semillero IA")
                .estado(Semillero.EstadoSemillero.ACTIVO)
                .build();

        PageResult<Semillero> pageResult = PageResult.<Semillero>builder()
                .contenido(List.of(semillero))
                .paginaActual(0)
                .tamano(15)
                .totalElementos(1L)
                .totalPaginas(1)
                .esUltimaPagina(true)
                .esPrimeraPagina(true)
                .build();

        when(consultarSemillerosUseCase.listarSemillerosActivos(any())).thenReturn(pageResult);
        when(semilleroRestMapper.toPageResponse(any())).thenCallRealMethod();

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/semilleros")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/semilleros/{id}: debe retornar 200 cuando el semillero existe")
    void obtenerDetalle_conIdExistente_retorna200() throws Exception {
        // ARRANGE
        Long idSemillero = 1L;
        Semillero semillero = Semillero.builder()
                .id(idSemillero)
                .nombre("Semillero Biotecnología")
                .estado(Semillero.EstadoSemillero.ACTIVO)
                .build();

        when(consultarSemillerosUseCase.obtenerDetalleSemillero(idSemillero)).thenReturn(semillero);
        when(semilleroRestMapper.toDetalleResponse(any())).thenCallRealMethod();

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/semilleros/{id}", idSemillero)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/semilleros/{id}: debe retornar 404 cuando el semillero no existe")
    void obtenerDetalle_conIdInexistente_retorna404() throws Exception {
        // ARRANGE
        Long idInexistente = 999L;
        when(consultarSemillerosUseCase.obtenerDetalleSemillero(idInexistente))
                .thenThrow(new RecursoNoEncontradoException("Semillero", idInexistente));

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/semilleros/{id}", idInexistente)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exitoso").value(false))
                .andExpect(jsonPath("$.codigoError").value("RECURSO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("GET /api/v1/semilleros: debe limitar el tamaño de página a 15")
    void listarSemilleros_conTamanoMayorA15_usaTamano15() throws Exception {
        // ARRANGE
        PageResult<Semillero> pageResult = PageResult.<Semillero>builder()
                .contenido(List.of())
                .paginaActual(0).tamano(15).totalElementos(0L).totalPaginas(0)
                .esUltimaPagina(true).esPrimeraPagina(true)
                .build();

        when(consultarSemillerosUseCase.listarSemillerosActivos(any())).thenReturn(pageResult);
        when(semilleroRestMapper.toPageResponse(any())).thenCallRealMethod();

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/semilleros")
                        .param("tamano", "100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
