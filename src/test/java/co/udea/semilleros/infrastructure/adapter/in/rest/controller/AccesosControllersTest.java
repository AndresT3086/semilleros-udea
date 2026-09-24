package co.udea.semilleros.infrastructure.adapter.in.rest.controller;

import co.udea.semilleros.domain.exception.ConflictoAccesoException;
import co.udea.semilleros.domain.exception.DominioCorreoNoPermitidoException;
import co.udea.semilleros.domain.exception.EnlaceInvalidoException;
import co.udea.semilleros.domain.exception.SolicitudAccesoInvalidaException;
import co.udea.semilleros.domain.model.acceso.DatosInvitacion;
import co.udea.semilleros.domain.model.acceso.EstadoSolicitud;
import co.udea.semilleros.domain.model.acceso.InvitacionEnviada;
import co.udea.semilleros.domain.model.acceso.SolicitudAcceso;
import co.udea.semilleros.domain.port.in.AdministrarAccesosUseCase;
import co.udea.semilleros.domain.port.in.RegistroCoordinadorUseCase;
import co.udea.semilleros.infrastructure.adapter.in.scheduler.AccesosScheduler;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({RegistroCoordinadorController.class, AdminAccesosController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, AccesosControllersTest.SeguridadPorMetodo.class})
@DisplayName("Registro de coordinadores y administración de accesos - Capa web")
class AccesosControllersTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class SeguridadPorMetodo {
    }

    @Autowired private MockMvc mockMvc;
    @MockBean private RegistroCoordinadorUseCase registroCoordinadorUseCase;
    @MockBean private AdministrarAccesosUseCase administrarAccesosUseCase;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    private static final String SOLICITUD = """
            {"nombres": "Ana", "apellidos": "Zapata", "cedula": "1040123456", "correo": "ana@udea.edu.co",
             "idUnidadAcademica": 1, "justificacion": "Coordino el semillero de inteligencia artificial",
             "sitioWeb": "", "respuestaMath": 7, "operando1": 3, "operando2": 4}
            """;

    private static void autenticarComo(String rol) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UsuarioPrincipal(6L, "yiyi.lopez@udea.edu.co", rol), null, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    // ─── Público ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /solicitudes-acceso: responde 202 con un mensaje genérico y registra la IP de origen")
    void solicitarAcceso() throws Exception {
        mockMvc.perform(post("/api/v1/solicitudes-acceso").contentType(MediaType.APPLICATION_JSON).content(SOLICITUD)
                        .header("X-Forwarded-For", "203.0.113.7, 10.0.0.1"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensaje").value(RegistroCoordinadorController.MENSAJE_SOLICITUD));
        verify(registroCoordinadorUseCase).solicitarAcceso(argThat(d -> d != null
                && d.correo().equals("ana@udea.edu.co") && d.ip().equals("203.0.113.7") && d.respuestaMath() == 7));
    }

    @Test
    @DisplayName("POST /solicitudes-acceso: valida el formulario y traduce dominio no permitido y captcha")
    void solicitarAcceso_validaciones() throws Exception {
        mockMvc.perform(post("/api/v1/solicitudes-acceso").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\": \"12\", \"justificacion\": \"corta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.datos.cedula").exists())
                .andExpect(jsonPath("$.datos.justificacion").exists())
                .andExpect(jsonPath("$.datos.correo").exists());
        mockMvc.perform(post("/api/v1/solicitudes-acceso").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoError").value("CUERPO_INVALIDO"));
        doThrow(new DominioCorreoNoPermitidoException("ana@gmail.com")).when(registroCoordinadorUseCase).solicitarAcceso(any());
        mockMvc.perform(post("/api/v1/solicitudes-acceso").contentType(MediaType.APPLICATION_JSON).content(SOLICITUD))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /solicitudes-acceso/verificar y /cuenta/activar: enlace válido 200, inválido 400")
    void verificarYActivar() throws Exception {
        mockMvc.perform(post("/api/v1/solicitudes-acceso/verificar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"abc\"}"))
                .andExpect(status().isOk());
        verify(registroCoordinadorUseCase).verificarCorreo("abc");

        mockMvc.perform(post("/api/v1/cuenta/activar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"abc\", \"contrasena\": \"Semilleros2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Cuenta activada. Ya puedes iniciar sesión."));
        verify(registroCoordinadorUseCase).activarCuenta("abc", "Semilleros2026");

        doThrow(new EnlaceInvalidoException()).when(registroCoordinadorUseCase).verificarCorreo("viejo");
        mockMvc.perform(post("/api/v1/solicitudes-acceso/verificar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"viejo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoError").value("ENLACE_INVALIDO"));
        doThrow(new SolicitudAccesoInvalidaException("débil")).when(registroCoordinadorUseCase).activarCuenta("abc", "corta");
        mockMvc.perform(post("/api/v1/cuenta/activar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"abc\", \"contrasena\": \"corta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoError").value("SOLICITUD_ACCESO_INVALIDA"));
        mockMvc.perform(post("/api/v1/cuenta/activar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ─── Administrador ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /admin/solicitudes-acceso: lista sin exponer token ni IP; resumen de pendientes")
    void listarYResumen() throws Exception {
        autenticarComo("ADMIN");
        when(administrarAccesosUseCase.listarSolicitudes(EstadoSolicitud.PENDIENTE)).thenReturn(List.of(
                SolicitudAcceso.builder().id(5L).nombres("Ana").apellidos("Zapata").correo("ana@udea.edu.co")
                        .cedula("1040123456").justificacion("x").estado(EstadoSolicitud.PENDIENTE)
                        .tokenHash("secreto").ipOrigen("10.0.0.1").fechaCreacion(Instant.parse("2026-09-24T15:00:00Z")).build()));
        when(administrarAccesosUseCase.contarPendientes()).thenReturn(1L);

        mockMvc.perform(get("/api/v1/admin/solicitudes-acceso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].correo").value("ana@udea.edu.co"))
                .andExpect(jsonPath("$.datos[0].tokenHash").doesNotExist())
                .andExpect(jsonPath("$.datos[0].ipOrigen").doesNotExist());
        mockMvc.perform(get("/api/v1/admin/solicitudes-acceso/resumen"))
                .andExpect(jsonPath("$.datos.pendientes").value(1));
        mockMvc.perform(get("/api/v1/admin/solicitudes-acceso").param("estado", "INVENTADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoError").value("PARAMETRO_INVALIDO"));
    }

    @Test
    @DisplayName("POST aprobar/rechazar: usan el administrador autenticado; conflictos retornan 409")
    void aprobarYRechazar() throws Exception {
        autenticarComo("ADMIN");
        when(administrarAccesosUseCase.aprobar(5L, 6L)).thenReturn(true);
        mockMvc.perform(post("/api/v1/admin/solicitudes-acceso/5/aprobar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Solicitud aprobada. Se envió el enlace de activación."))
                .andExpect(jsonPath("$.datos.correoEnviado").value(true));
        verify(administrarAccesosUseCase).aprobar(5L, 6L);
        mockMvc.perform(post("/api/v1/admin/solicitudes-acceso/8/aprobar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.startsWith("La solicitud quedó aprobada, pero no se pudo enviar")))
                .andExpect(jsonPath("$.datos.correoEnviado").value(false));

        mockMvc.perform(post("/api/v1/admin/solicitudes-acceso/5/rechazar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"No coordina semilleros\", \"bloquear\": true}"))
                .andExpect(status().isOk());
        verify(administrarAccesosUseCase).rechazar(5L, 6L, "No coordina semilleros", true);

        mockMvc.perform(post("/api/v1/admin/solicitudes-acceso/5/rechazar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"\"}"))
                .andExpect(status().isBadRequest());

        doThrow(new ConflictoAccesoException("ya revisada")).when(administrarAccesosUseCase).aprobar(7L, 6L);
        mockMvc.perform(post("/api/v1/admin/solicitudes-acceso/7/aprobar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigoError").value("CONFLICTO_ACCESO"));
    }

    @Test
    @DisplayName("POST /admin/invitaciones: 201 al invitar; correos fuera de @udea.edu.co retornan 400")
    void invitar() throws Exception {
        autenticarComo("ADMIN");
        when(administrarAccesosUseCase.invitar(eq(new DatosInvitacion("Ana", "Zapata", "ana@udea.edu.co")), eq(6L)))
                .thenReturn(new InvitacionEnviada(21L, "ana@udea.edu.co", Instant.parse("2026-09-25T15:00:00Z"), true, true));
        when(administrarAccesosUseCase.invitar(eq(new DatosInvitacion("Luis", "Mora", "luis@udea.edu.co")), eq(6L)))
                .thenReturn(new InvitacionEnviada(22L, "luis@udea.edu.co", Instant.parse("2026-09-25T15:00:00Z"), false, false));
        when(administrarAccesosUseCase.invitar(eq(new DatosInvitacion("Ana", "Zapata", "ana@gmail.com")), eq(6L)))
                .thenThrow(new DominioCorreoNoPermitidoException("ana@gmail.com"));

        mockMvc.perform(post("/api/v1/admin/invitaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\": \"Ana\", \"apellidos\": \"Zapata\", \"correo\": \"ana@udea.edu.co\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Invitación reenviada."))
                .andExpect(jsonPath("$.datos.idUsuario").value(21))
                .andExpect(jsonPath("$.datos.correoEnviado").value(true));
        mockMvc.perform(post("/api/v1/admin/invitaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\": \"Luis\", \"apellidos\": \"Mora\", \"correo\": \"luis@udea.edu.co\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.startsWith("La invitación quedó registrada, pero no se pudo enviar el correo")))
                .andExpect(jsonPath("$.datos.correoEnviado").value(false));
        mockMvc.perform(post("/api/v1/admin/invitaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\": \"Ana\", \"apellidos\": \"Zapata\", \"correo\": \"ana@gmail.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoError").value("DOMINIO_CORREO_NO_PERMITIDO"));
    }

    @Test
    @DisplayName("Un coordinador no puede ver solicitudes, aprobar ni invitar")
    void coordinador_recibe403() throws Exception {
        autenticarComo("COORDINADOR");

        mockMvc.perform(get("/api/v1/admin/solicitudes-acceso")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/invitaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\": \"A\", \"apellidos\": \"B\", \"correo\": \"x@udea.edu.co\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(administrarAccesosUseCase);
    }

    @Test
    @DisplayName("AccesosScheduler: delega la limpieza y el resumen diario en el caso de uso")
    void scheduler() {
        AdministrarAccesosUseCase useCase = mock(AdministrarAccesosUseCase.class);
        when(useCase.limpiarVencidos()).thenReturn(0).thenReturn(4);
        AccesosScheduler scheduler = new AccesosScheduler(useCase);

        scheduler.limpiarVencidos();
        scheduler.limpiarVencidos();
        scheduler.enviarResumenPendientes();

        verify(useCase, org.mockito.Mockito.times(2)).limpiarVencidos();
        verify(useCase).enviarResumenPendientes();
    }
}
