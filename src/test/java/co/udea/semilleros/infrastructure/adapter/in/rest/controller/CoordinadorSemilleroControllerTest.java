package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.exception.AccesoNoAutorizadoException;
import co.udea.semilleros.domain.exception.CamposObligatoriosPendientesException;
import co.udea.semilleros.domain.exception.RecursoNoEncontradoException;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.port.in.GestionarSemilleroUseCase;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.PestanaDofaResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.PestanaGeneralResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.SemilleroDetalleResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.mapper.SemilleroRestMapper;
import co.udea.semilleros.infrastructure.config.GlobalExceptionHandler;
import co.udea.semilleros.infrastructure.security.filter.UsuarioPrincipal;
import co.udea.semilleros.infrastructure.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoordinadorSemilleroController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("CoordinadorSemilleroController - Pruebas de integración de capa web")
class CoordinadorSemilleroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GestionarSemilleroUseCase gestionarSemilleroUseCase;

    @MockBean
    private SemilleroRestMapper semilleroRestMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final UsuarioPrincipal PRINCIPAL =
            new UsuarioPrincipal(5L, "coordinador@udea.edu.co", "COORDINADOR");

    // Con addFilters=false la cadena de seguridad real no corre (ni JwtAuthenticationFilter
    // ni el TestSecurityContextHolderFilter de spring-security-test), así que el
    // SecurityContextHolder (ThreadLocal) se puebla directamente para que
    // @AuthenticationPrincipal lo resuelva durante la invocación del controlador.
    @BeforeEach
    void autenticarComoCoordinador() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                PRINCIPAL, null, List.of(new SimpleGrantedAuthority("ROLE_COORDINADOR"))));
    }

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /iniciar: debe crear un borrador y retornar 201")
    void iniciarCaracterizacion_retorna201() throws Exception {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).codigo("SEM-UDEA-0001").build();
        when(gestionarSemilleroUseCase.crearSemilleroBorrador(5L)).thenReturn(semillero);
        when(semilleroRestMapper.toDetalleResponse(semillero))
                .thenReturn(SemilleroDetalleResponse.builder().id(1L).codigo("SEM-UDEA-0001").build());

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/coordinador/semilleros/iniciar"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true));
    }

    @Test
    @DisplayName("GET /mis-semilleros: debe retornar los semilleros del coordinador autenticado")
    void obtenerMisSemilleros_retorna200() throws Exception {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).build();
        when(gestionarSemilleroUseCase.obtenerSemillerosDelCoordinador(5L)).thenReturn(List.of(semillero));
        when(semilleroRestMapper.toDetalleResponse(semillero))
                .thenReturn(SemilleroDetalleResponse.builder().id(1L).build());

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/coordinador/semilleros/mis-semilleros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("GET /{idSemillero}: debe retornar 403 cuando el coordinador no es dueño")
    void obtenerSemillero_noPropietario_retorna403() throws Exception {
        // ARRANGE
        when(gestionarSemilleroUseCase.obtenerSemilleroDelCoordinadorPorId(1L, 5L))
                .thenThrow(new AccesoNoAutorizadoException("semillero con id 1"));

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/coordinador/semilleros/{idSemillero}", 1L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.exitoso").value(false));
    }

    @Test
    @DisplayName("GET /{idSemillero}: debe retornar 404 cuando el semillero no existe")
    void obtenerSemillero_inexistente_retorna404() throws Exception {
        // ARRANGE
        when(gestionarSemilleroUseCase.obtenerSemilleroDelCoordinadorPorId(999L, 5L))
                .thenThrow(new RecursoNoEncontradoException("Semillero", 999L));

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/coordinador/semilleros/{idSemillero}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /pestana/general: debe guardar la pestaña y retornar 200")
    void guardarPestanaGeneral_conDatosValidos_retorna200() throws Exception {
        // ARRANGE
        Semillero datos = Semillero.builder().nombre("Semillero IA").build();
        Semillero guardado = Semillero.builder().id(1L).nombre("Semillero IA").build();

        when(semilleroRestMapper.toPestanaGeneralDomain(any())).thenReturn(datos);
        when(gestionarSemilleroUseCase.guardarPestanaGeneral(eq(1L), eq(5L), any())).thenReturn(guardado);
        when(semilleroRestMapper.toDetalleResponse(guardado))
                .thenReturn(SemilleroDetalleResponse.builder().id(1L).nombre("Semillero IA").build());

        String body = """
                {
                  "nombre": "Semillero IA",
                  "siglas": "SIA",
                  "correoSemillero": "ia@udea.edu.co",
                  "telefono": "3001234567",
                  "anioCreacion": 2024,
                  "mision": "Misión",
                  "vision": "Visión",
                  "objetivo": "Objetivo",
                  "lineasInvestigacion": "Líneas",
                  "palabrasClave": "IA, ML",
                  "grupoInvestigacion": "Grupo IA",
                  "idUnidadAcademica": 1,
                  "idCampus": 1,
                  "idAreaOcde": 1
                }
                """;

        // ACT & ASSERT
        mockMvc.perform(patch("/api/v1/coordinador/semilleros/{idSemillero}/pestana/general", 1L)
                        
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));
    }

    @Test
    @DisplayName("PATCH /pestana/general: debe retornar 400 cuando faltan campos obligatorios en el request")
    void guardarPestanaGeneral_conCamposFaltantes_retorna400() throws Exception {
        // ACT & ASSERT: 'nombre' y 'correoSemillero' faltan -> falla @Valid antes de llegar al caso de uso
        mockMvc.perform(patch("/api/v1/coordinador/semilleros/{idSemillero}/pestana/general", 1L)
                        
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /pestana/produccion: debe guardar la pestaña y retornar 200")
    void guardarProduccion_retorna200() throws Exception {
        // ARRANGE
        Semillero guardado = Semillero.builder().id(1L).estadoCaracterizacion("PRODUCCION_COMPLETADO").build();
        when(gestionarSemilleroUseCase.guardarPestanaProduccion(
                eq(1L), eq(5L), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(guardado);
        when(semilleroRestMapper.toDetalleResponse(guardado))
                .thenReturn(SemilleroDetalleResponse.builder().id(1L).build());

        // ACT & ASSERT
        mockMvc.perform(patch("/api/v1/coordinador/semilleros/{idSemillero}/pestana/produccion", 1L)
                        
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tienenArticulos\": true, \"cantidadArticulos\": 3}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /pestana/organizacion: debe retornar 400 cuando no hay recursos ni fuentes")
    void guardarOrganizacion_sinDatos_retorna400() throws Exception {
        // ACT & ASSERT: listas vacías -> falla @NotEmpty antes del caso de uso
        mockMvc.perform(patch("/api/v1/coordinador/semilleros/{idSemillero}/pestana/organizacion", 1L)
                        
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idsRecursos\": [], \"idsFuentesFinanciacion\": []}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /pestana/organizacion: debe guardar la pestaña y retornar 200")
    void guardarOrganizacion_conDatosValidos_retorna200() throws Exception {
        // ARRANGE
        Semillero guardado = Semillero.builder().id(1L).estadoCaracterizacion("ORGANIZACION_COMPLETADO").build();
        when(gestionarSemilleroUseCase.guardarPestanaOrganizacion(eq(1L), eq(5L), any(), any()))
                .thenReturn(guardado);
        when(semilleroRestMapper.toDetalleResponse(guardado))
                .thenReturn(SemilleroDetalleResponse.builder().id(1L).build());

        // ACT & ASSERT
        mockMvc.perform(patch("/api/v1/coordinador/semilleros/{idSemillero}/pestana/organizacion", 1L)
                        
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idsRecursos\": [1], \"idsFuentesFinanciacion\": [2]}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /pestana/relacionamiento: debe guardar la pestaña y retornar 200")
    void guardarRelacionamiento_retorna200() throws Exception {
        // ARRANGE
        Semillero guardado = Semillero.builder().id(1L).estadoCaracterizacion("RELACIONAMIENTO_COMPLETADO").build();
        when(gestionarSemilleroUseCase.guardarPestanaRelacionamiento(
                eq(1L), eq(5L), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(guardado);
        when(semilleroRestMapper.toDetalleResponse(guardado))
                .thenReturn(SemilleroDetalleResponse.builder().id(1L).build());

        // ACT & ASSERT
        mockMvc.perform(patch("/api/v1/coordinador/semilleros/{idSemillero}/pestana/relacionamiento", 1L)
                        
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adscritoGrupo\": false}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /pestana/actividades: debe guardar la pestaña y retornar 200")
    void guardarActividades_retorna200() throws Exception {
        // ARRANGE
        Semillero guardado = Semillero.builder().id(1L).estadoCaracterizacion("ACTIVIDADES_COMPLETADO").build();
        when(gestionarSemilleroUseCase.guardarPestanaActividades(eq(1L), eq(5L), any()))
                .thenReturn(guardado);
        when(semilleroRestMapper.toDetalleResponse(guardado))
                .thenReturn(SemilleroDetalleResponse.builder().id(1L).build());

        // ACT & ASSERT
        mockMvc.perform(patch("/api/v1/coordinador/semilleros/{idSemillero}/pestana/actividades", 1L)
                        
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actividades\": [{\"idActividad\": 1, \"realiza\": true}]}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /pestana/dofa: debe guardar la pestaña y retornar 200")
    void guardarDofa_retorna200() throws Exception {
        // ARRANGE
        Semillero guardado = Semillero.builder().id(1L).estadoCaracterizacion("DOFA_COMPLETADO").build();
        when(gestionarSemilleroUseCase.guardarPestanaDofa(eq(1L), eq(5L), any(), any(), any(), any()))
                .thenReturn(guardado);
        when(semilleroRestMapper.toDetalleResponse(guardado))
                .thenReturn(SemilleroDetalleResponse.builder().id(1L).build());

        String body = "{\"fortalezas\":\"F\",\"debilidades\":\"D\",\"oportunidades\":\"O\",\"amenazas\":\"A\"}";

        // ACT & ASSERT
        mockMvc.perform(patch("/api/v1/coordinador/semilleros/{idSemillero}/pestana/dofa", 1L)
                        
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /pestana/dofa: debe retornar 400 cuando faltan campos obligatorios")
    void guardarDofa_conCamposFaltantes_retorna400() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(patch("/api/v1/coordinador/semilleros/{idSemillero}/pestana/dofa", 1L)
                        
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /pestana/ods: debe guardar la pestaña y retornar 200")
    void guardarOds_retorna200() throws Exception {
        // ARRANGE
        Semillero guardado = Semillero.builder().id(1L).estadoCaracterizacion("ODS_COMPLETADO").build();
        when(gestionarSemilleroUseCase.guardarPestanaOds(eq(1L), eq(5L), any(), any(), any(), any()))
                .thenReturn(guardado);
        when(semilleroRestMapper.toDetalleResponse(guardado))
                .thenReturn(SemilleroDetalleResponse.builder().id(1L).build());

        // ACT & ASSERT
        mockMvc.perform(patch("/api/v1/coordinador/semilleros/{idSemillero}/pestana/ods", 1L)
                        
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idAreaOcde\": 1, \"idOdsPrincipal\": 2}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /pestana/general: debe retornar los datos de la pestaña General")
    void obtenerGeneral_retorna200() throws Exception {
        // ARRANGE
        when(gestionarSemilleroUseCase.obtenerPestanaGeneral(1L, 5L))
                .thenReturn(PestanaGeneralResponse.builder().id(1L).nombre("Semillero IA").build());

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/coordinador/semilleros/{idSemillero}/pestana/general", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.nombre").value("Semillero IA"));
    }

    @Test
    @DisplayName("GET /pestana/dofa: debe retornar los datos de la pestaña DOFA")
    void obtenerDofa_retorna200() throws Exception {
        // ARRANGE
        when(gestionarSemilleroUseCase.obtenerPestanaDofa(1L, 5L))
                .thenReturn(PestanaDofaResponse.builder().fortalezas("F").build());

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/coordinador/semilleros/{idSemillero}/pestana/dofa", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.fortalezas").value("F"));
    }

    @Test
    @DisplayName("POST /finalizar: debe retornar 400 cuando hay pestañas incompletas")
    void finalizarCaracterizacion_conPestanasIncompletas_retorna400() throws Exception {
        // ARRANGE
        when(gestionarSemilleroUseCase.finalizarCaracterizacion(1L, 5L))
                .thenThrow(new CamposObligatoriosPendientesException("Finalización", List.of("Pestaña DOFA incompleta")));

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/coordinador/semilleros/{idSemillero}/finalizar", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /finalizar: debe retornar 200 cuando la caracterización se completa")
    void finalizarCaracterizacion_conDatosValidos_retorna200() throws Exception {
        // ARRANGE
        Semillero finalizado = Semillero.builder().id(1L).estado(Semillero.EstadoSemillero.ACTIVO).build();
        when(gestionarSemilleroUseCase.finalizarCaracterizacion(1L, 5L)).thenReturn(finalizado);
        when(semilleroRestMapper.toDetalleResponse(finalizado))
                .thenReturn(SemilleroDetalleResponse.builder().id(1L).estado("ACTIVO").build());

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/coordinador/semilleros/{idSemillero}/finalizar", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));
    }
}
