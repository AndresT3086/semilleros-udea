package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.exception.InscripcionDuplicadaException;
import co.udea.semilleros.domain.exception.RecursoNoEncontradoException;
import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.port.in.InscribirseASemilleroUseCase;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.InscripcionResponse;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InscripcionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("InscripcionController - Pruebas de integración de capa web")
class InscripcionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InscribirseASemilleroUseCase inscribirseASemilleroUseCase;

    @MockBean
    private SemilleroRestMapper semilleroRestMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final String BODY_VALIDO = """
            {
              "idSemillero": 1,
              "nombres": "Juan",
              "apellidos": "Pérez",
              "cedula": "1040123456",
              "correo": "juan.perez@gmail.com",
              "telefono": "3001234567",
              "aceptaTerminos": true
            }
            """;

    @Test
    @DisplayName("POST /inscripciones: debe retornar 201 cuando la inscripción es válida, sin importar el dominio del correo")
    void inscribirse_conDatosValidos_retorna201() throws Exception {
        // ARRANGE
        Inscripcion dominio = Inscripcion.builder().idSemillero(1L).correo("juan.perez@gmail.com").build();
        Inscripcion guardada = dominio.withId(1L).withEstado(Inscripcion.EstadoInscripcion.PENDIENTE);

        when(semilleroRestMapper.toInscripcionDomain(any())).thenReturn(dominio);
        when(inscribirseASemilleroUseCase.inscribir(dominio)).thenReturn(guardada);
        when(semilleroRestMapper.toInscripcionResponse(guardada))
                .thenReturn(InscripcionResponse.builder().id(1L).estado("PENDIENTE").build());

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/inscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("POST /inscripciones: debe retornar 409 cuando ya existe una inscripción activa")
    void inscribirse_conInscripcionDuplicada_retorna409() throws Exception {
        // ARRANGE
        Inscripcion dominio = Inscripcion.builder().idSemillero(1L).correo("juan.perez@gmail.com").build();
        when(semilleroRestMapper.toInscripcionDomain(any())).thenReturn(dominio);
        when(inscribirseASemilleroUseCase.inscribir(dominio))
                .thenThrow(new InscripcionDuplicadaException("juan.perez@gmail.com", 1L));

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/inscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.exitoso").value(false));
    }

    @Test
    @DisplayName("POST /inscripciones: debe retornar 404 cuando el semillero no existe")
    void inscribirse_conSemilleroInexistente_retorna404() throws Exception {
        // ARRANGE
        Inscripcion dominio = Inscripcion.builder().idSemillero(1L).correo("juan.perez@gmail.com").build();
        when(semilleroRestMapper.toInscripcionDomain(any())).thenReturn(dominio);
        when(inscribirseASemilleroUseCase.inscribir(dominio))
                .thenThrow(new RecursoNoEncontradoException("Semillero", 1L));

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/inscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /inscripciones: debe retornar 400 cuando no se aceptan los términos y condiciones")
    void inscribirse_sinAceptarTerminos_retorna400() throws Exception {
        // ACT & ASSERT
        String body = """
                {
                  "idSemillero": 1,
                  "nombres": "Juan",
                  "apellidos": "Pérez",
                  "cedula": "1040123456",
                  "correo": "juan.perez@udea.edu.co",
                  "telefono": "3001234567",
                  "aceptaTerminos": false
                }
                """;

        mockMvc.perform(post("/api/v1/inscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /inscripciones: debe retornar 400 cuando el sexo no es un valor permitido")
    void inscribirse_conSexoInvalido_retorna400() throws Exception {
        String body = BODY_VALIDO.replace("\"aceptaTerminos\": true", "\"aceptaTerminos\": true, \"sexo\": \"X\"");

        mockMvc.perform(post("/api/v1/inscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.datos.sexo").exists());
    }

    @Test
    @DisplayName("POST /inscripciones: el sexo vacío se envía como no informado (null)")
    void inscribirse_conSexoVacio_loNormalizaANull() throws Exception {
        Inscripcion dominio = Inscripcion.builder().idSemillero(1L).build();
        when(semilleroRestMapper.toInscripcionDomain(argThat(request -> request != null && request.getSexo() == null)))
                .thenReturn(dominio);
        when(inscribirseASemilleroUseCase.inscribir(dominio)).thenReturn(dominio.withId(1L));
        when(semilleroRestMapper.toInscripcionResponse(any()))
                .thenReturn(InscripcionResponse.builder().id(1L).estado("PENDIENTE").build());
        String body = BODY_VALIDO.replace("\"aceptaTerminos\": true", "\"aceptaTerminos\": true, \"sexo\": \"\"");

        mockMvc.perform(post("/api/v1/inscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}
