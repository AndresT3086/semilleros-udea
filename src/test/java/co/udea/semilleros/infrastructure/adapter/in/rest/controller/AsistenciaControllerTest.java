package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.exception.AccesoNoAutorizadoException;
import co.udea.semilleros.domain.exception.DatosAsistenciaInvalidosException;
import co.udea.semilleros.domain.model.asistencia.ConteoAsistencia;
import co.udea.semilleros.domain.model.asistencia.DatosSesion;
import co.udea.semilleros.domain.model.asistencia.EstadoAsistencia;
import co.udea.semilleros.domain.model.asistencia.IntegranteAsistencia;
import co.udea.semilleros.domain.model.asistencia.Sesion;
import co.udea.semilleros.domain.model.asistencia.SesionDetalle;
import co.udea.semilleros.domain.port.in.GestionarAsistenciaUseCase;
import co.udea.semilleros.infrastructure.config.GlobalExceptionHandler;
import co.udea.semilleros.infrastructure.security.filter.UsuarioPrincipal;
import co.udea.semilleros.infrastructure.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsistenciaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, AsistenciaControllerTest.SeguridadPorMetodo.class})
@DisplayName("AsistenciaController - Pruebas de capa web")
class AsistenciaControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class SeguridadPorMetodo {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GestionarAsistenciaUseCase gestionarAsistenciaUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final Sesion SESION = new Sesion(3L, 10L, 1L, "Seminarios", "Club de revista",
            LocalDate.of(2026, 9, 20), new ConteoAsistencia(8, 1, 1));
    private static final SesionDetalle DETALLE = new SesionDetalle(SESION,
            List.of(new SesionDetalle.Asistente(1L, "Ana Zapata", "111", EstadoAsistencia.PRESENTE)));
    private static final String BODY = """
            {"titulo": "Club de revista", "fecha": "2026-09-20", "idActividad": 1,
             "asistencias": [{"idIntegrante": 1, "estado": "PRESENTE"}, {"idIntegrante": 2, "estado": "EXCUSADO"}]}
            """;

    private static void autenticarComo(String rol) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UsuarioPrincipal(5L, "c@udea.edu.co", rol), null, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /{id}/sesiones: lista actividades con el % de asistencia de cada una")
    void listarSesiones() throws Exception {
        autenticarComo("COORDINADOR");
        when(gestionarAsistenciaUseCase.listarSesiones(5L, 10L, "2026-2")).thenReturn(List.of(SESION));

        mockMvc.perform(get("/api/v1/coordinador/semilleros/10/sesiones").param("periodo", "2026-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].titulo").value("Club de revista"))
                .andExpect(jsonPath("$.datos[0].asistencia.presentes").value(8))
                .andExpect(jsonPath("$.datos[0].asistencia.excusados").value(1))
                .andExpect(jsonPath("$.datos[0].asistencia.porcentaje").value(88.9));
    }

    @Test
    @DisplayName("POST /{id}/sesiones: registra la actividad y retorna 201 con la lista")
    void registrarSesion() throws Exception {
        autenticarComo("COORDINADOR");
        when(gestionarAsistenciaUseCase.registrarSesion(eq(5L), eq(10L), argThat(datos -> datos != null
                && datos.fecha().equals(LocalDate.of(2026, 9, 20))
                && datos.asistencias().equals(List.of(new DatosSesion.Registro(1L, EstadoAsistencia.PRESENTE),
                new DatosSesion.Registro(2L, EstadoAsistencia.EXCUSADO))))))
                .thenReturn(DETALLE);

        mockMvc.perform(post("/api/v1/coordinador/semilleros/10/sesiones").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.sesion.id").value(3))
                .andExpect(jsonPath("$.datos.asistencias[0].estado").value("PRESENTE"));
    }

    @Test
    @DisplayName("POST /{id}/sesiones: valida título, fecha y estados")
    void registrarSesion_validaciones() throws Exception {
        autenticarComo("COORDINADOR");

        mockMvc.perform(post("/api/v1/coordinador/semilleros/10/sesiones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \" \", \"asistencias\": [{\"idIntegrante\": 1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.datos.titulo").exists())
                .andExpect(jsonPath("$.datos.fecha").exists());
        verifyNoInteractions(gestionarAsistenciaUseCase);
    }

    @Test
    @DisplayName("POST /{id}/sesiones: reglas de negocio inválidas retornan 400 y semillero ajeno 403")
    void registrarSesion_erroresDeNegocio() throws Exception {
        autenticarComo("COORDINADOR");
        when(gestionarAsistenciaUseCase.registrarSesion(eq(5L), eq(10L), any()))
                .thenThrow(new DatosAsistenciaInvalidosException("No se puede registrar asistencia de una fecha futura."));
        when(gestionarAsistenciaUseCase.registrarSesion(eq(5L), eq(11L), any()))
                .thenThrow(new AccesoNoAutorizadoException("asistencia del semillero 11"));

        mockMvc.perform(post("/api/v1/coordinador/semilleros/10/sesiones").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoError").value("DATOS_ASISTENCIA_INVALIDOS"));
        mockMvc.perform(post("/api/v1/coordinador/semilleros/11/sesiones").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET, PUT y DELETE /sesiones/{id}: consultan, corrigen y eliminan la actividad")
    void detalleActualizarEliminar() throws Exception {
        autenticarComo("COORDINADOR");
        when(gestionarAsistenciaUseCase.obtenerSesion(5L, 3L)).thenReturn(DETALLE);
        when(gestionarAsistenciaUseCase.actualizarSesion(eq(5L), eq(3L), any())).thenReturn(DETALLE);

        mockMvc.perform(get("/api/v1/coordinador/semilleros/sesiones/3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.datos.asistencias[0].nombre").value("Ana Zapata"));
        mockMvc.perform(put("/api/v1/coordinador/semilleros/sesiones/3").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mensaje").value("Actividad actualizada."));
        mockMvc.perform(delete("/api/v1/coordinador/semilleros/sesiones/3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mensaje").value("Actividad eliminada."));
        verify(gestionarAsistenciaUseCase).eliminarSesion(5L, 3L);
    }

    @Test
    @DisplayName("GET /{id}/asistencia/integrantes: % por integrante, null cuando no hay asistencias esperadas")
    void asistenciaPorIntegrante() throws Exception {
        autenticarComo("COORDINADOR");
        when(gestionarAsistenciaUseCase.asistenciaPorIntegrante(eq(5L), eq(10L), isNull())).thenReturn(List.of(
                new IntegranteAsistencia(1L, "Ana Zapata", "111", true, new ConteoAsistencia(3, 1, 2)),
                new IntegranteAsistencia(2L, "Beto Arias", "222", true, ConteoAsistencia.VACIO)));

        mockMvc.perform(get("/api/v1/coordinador/semilleros/10/asistencia/integrantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].asistencia.porcentaje").value(75.0))
                .andExpect(jsonPath("$.datos[1].asistencia.porcentaje").doesNotExist());
    }

    @Test
    @DisplayName("El administrador no registra asistencia (solo el coordinador)")
    void administrador_recibe403() throws Exception {
        autenticarComo("ADMIN");

        mockMvc.perform(post("/api/v1/coordinador/semilleros/10/sesiones").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(gestionarAsistenciaUseCase);
    }
}
