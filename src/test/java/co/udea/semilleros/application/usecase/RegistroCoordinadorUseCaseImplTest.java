package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.DominioCorreoNoPermitidoException;
import co.udea.semilleros.domain.exception.EnlaceInvalidoException;
import co.udea.semilleros.domain.exception.SolicitudAccesoInvalidaException;
import co.udea.semilleros.domain.exception.ValidacionBotException;
import co.udea.semilleros.domain.model.Usuario;
import co.udea.semilleros.domain.model.acceso.DatosSolicitud;
import co.udea.semilleros.domain.model.acceso.EstadoSolicitud;
import co.udea.semilleros.domain.model.acceso.SolicitudAcceso;
import co.udea.semilleros.domain.model.acceso.TokenCuenta;
import co.udea.semilleros.domain.model.acceso.TokenGenerado;
import co.udea.semilleros.domain.port.out.NotificacionEmailPort;
import co.udea.semilleros.domain.port.out.SolicitudAccesoRepositoryPort;
import co.udea.semilleros.domain.port.out.TokenCuentaRepositoryPort;
import co.udea.semilleros.domain.port.out.TokenSeguroPort;
import co.udea.semilleros.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistroCoordinadorUseCaseImpl - Solicitud, verificación y activación")
class RegistroCoordinadorUseCaseImplTest {

    private static final Instant AHORA = Instant.parse("2026-09-24T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneId.of("America/Bogota"));
    private static final String CORREO = "ana.zapata@udea.edu.co";
    private static final TokenGenerado TOKEN = new TokenGenerado("valor-token", "hash-token");

    @Mock private SolicitudAccesoRepositoryPort solicitudes;
    @Mock private TokenCuentaRepositoryPort tokensCuenta;
    @Mock private UsuarioRepositoryPort usuarios;
    @Mock private TokenSeguroPort tokenSeguro;
    @Mock private NotificacionEmailPort notificaciones;
    @Mock private PasswordEncoder passwordEncoder;

    private RegistroCoordinadorUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegistroCoordinadorUseCaseImpl(solicitudes, tokensCuenta, usuarios, tokenSeguro,
                notificaciones, passwordEncoder, CLOCK);
        ReflectionTestUtils.setField(useCase, "dominioPermitido", "@udea.edu.co");
        ReflectionTestUtils.setField(useCase, "horasVerificacion", 1L);
        ReflectionTestUtils.setField(useCase, "minutosEntreEnvios", 10L);
        ReflectionTestUtils.setField(useCase, "maxEnviosDia", 3);
        ReflectionTestUtils.setField(useCase, "diasEsperaRechazo", 30L);
    }

    private static DatosSolicitud datos(String correo) {
        return new DatosSolicitud(" Ana ", " Zapata ", "1040123456", correo, 3L,
                " Coordino el semillero de IA ", "", 7, 3, 4, "10.0.0.1");
    }

    private static SolicitudAcceso enCurso(EstadoSolicitud estado, Instant ultimoEnvio, int envios) {
        return SolicitudAcceso.builder().id(5L).nombres("Ana").apellidos("Zapata").cedula("1040123456")
                .correo(CORREO).justificacion("x").estado(estado).enviosVerificacion(envios)
                .ultimoEnvio(ultimoEnvio).build();
    }

    // ─── solicitarAcceso ──────────────────────────────────────────────────────

    @Test
    @DisplayName("crea la solicitud sin verificar, con token de 1 hora, y envía el enlace")
    void solicitarAcceso_nueva() {
        when(usuarios.buscarPorCorreo(CORREO)).thenReturn(Optional.empty());
        when(solicitudes.buscarUltimaRechazada(CORREO, "1040123456")).thenReturn(Optional.empty());
        when(solicitudes.buscarEnCurso(CORREO, "1040123456")).thenReturn(Optional.empty());
        when(tokenSeguro.generar()).thenReturn(TOKEN);
        when(solicitudes.crear(any())).thenReturn(9L);

        useCase.solicitarAcceso(datos("  Ana.Zapata@UDEA.edu.co "));

        ArgumentCaptor<SolicitudAcceso> captor = ArgumentCaptor.forClass(SolicitudAcceso.class);
        verify(solicitudes).crear(captor.capture());
        SolicitudAcceso creada = captor.getValue();
        assertThat(creada.correo()).isEqualTo(CORREO);
        assertThat(creada.nombres()).isEqualTo("Ana");
        assertThat(creada.justificacion()).isEqualTo("Coordino el semillero de IA");
        assertThat(creada.estado()).isEqualTo(EstadoSolicitud.PENDIENTE_VERIFICACION);
        assertThat(creada.tokenHash()).isEqualTo("hash-token");
        assertThat(creada.tokenExpira()).isEqualTo(AHORA.plus(Duration.ofHours(1)));
        assertThat(creada.ipOrigen()).isEqualTo("10.0.0.1");
        verify(notificaciones).enviarVerificacionSolicitud(CORREO, "Ana Zapata", "valor-token");
    }

    @Test
    @DisplayName("si otra petición simultánea ya creó la solicitud, no envía un segundo correo")
    void solicitarAcceso_creacionConcurrente() {
        when(usuarios.buscarPorCorreo(CORREO)).thenReturn(Optional.empty());
        when(solicitudes.buscarUltimaRechazada(any(), any())).thenReturn(Optional.empty());
        when(solicitudes.buscarEnCurso(any(), any())).thenReturn(Optional.empty());
        when(tokenSeguro.generar()).thenReturn(TOKEN);
        when(solicitudes.crear(any())).thenReturn(null);

        useCase.solicitarAcceso(datos(CORREO));

        verifyNoInteractions(notificaciones);
    }

    @Test
    @DisplayName("rechaza correos fuera del dominio institucional y captcha incorrecto")
    void solicitarAcceso_dominioYCaptcha() {
        assertThatThrownBy(() -> useCase.solicitarAcceso(datos("ana@gmail.com")))
                .isInstanceOf(DominioCorreoNoPermitidoException.class);
        DatosSolicitud captchaMalo = new DatosSolicitud("Ana", "Zapata", "1040123456", CORREO, null, "x", "", 8, 3, 4, "ip");
        assertThatThrownBy(() -> useCase.solicitarAcceso(captchaMalo)).isInstanceOf(ValidacionBotException.class);
        verifyNoInteractions(solicitudes);
    }

    @Test
    @DisplayName("si el campo trampa viene lleno lo descarta en silencio")
    void solicitarAcceso_campoTrampa() {
        DatosSolicitud bot = new DatosSolicitud("Ana", "Zapata", "1040123456", "x@gmail.com", null, "x",
                "http://spam.example", 0, 1, 1, "ip");

        useCase.solicitarAcceso(bot);

        verifyNoInteractions(solicitudes, usuarios, notificaciones);
    }

    @Test
    @DisplayName("si el correo ya tiene cuenta no hace nada (sin revelarlo)")
    void solicitarAcceso_correoConCuenta() {
        when(usuarios.buscarPorCorreo(CORREO)).thenReturn(Optional.of(Usuario.builder().id(1L).build()));

        useCase.solicitarAcceso(datos(CORREO));

        verifyNoInteractions(solicitudes, notificaciones);
    }

    @Test
    @DisplayName("respeta la espera de 30 días tras un rechazo y el bloqueo permanente")
    void solicitarAcceso_rechazoReciente() {
        when(usuarios.buscarPorCorreo(CORREO)).thenReturn(Optional.empty());
        SolicitudAcceso rechazada = enCurso(EstadoSolicitud.RECHAZADA, null, 1).withFechaRevision(AHORA.minus(Duration.ofDays(29)));
        when(solicitudes.buscarUltimaRechazada(CORREO, "1040123456"))
                .thenReturn(Optional.of(rechazada))
                .thenReturn(Optional.of(rechazada.withFechaRevision(AHORA.minus(Duration.ofDays(400))).withBloqueada(true)));

        useCase.solicitarAcceso(datos(CORREO));
        useCase.solicitarAcceso(datos(CORREO));

        verify(solicitudes, never()).buscarEnCurso(any(), any());
        verifyNoInteractions(notificaciones);
    }

    @Test
    @DisplayName("tras la espera de un rechazo puede volver a solicitar")
    void solicitarAcceso_rechazoVencido() {
        when(usuarios.buscarPorCorreo(CORREO)).thenReturn(Optional.empty());
        when(solicitudes.buscarUltimaRechazada(any(), any()))
                .thenReturn(Optional.of(enCurso(EstadoSolicitud.RECHAZADA, null, 1).withFechaRevision(AHORA.minus(Duration.ofDays(31)))));
        when(solicitudes.buscarEnCurso(any(), any())).thenReturn(Optional.empty());
        when(tokenSeguro.generar()).thenReturn(TOKEN);
        when(solicitudes.crear(any())).thenReturn(10L);

        useCase.solicitarAcceso(datos(CORREO));

        verify(notificaciones).enviarVerificacionSolicitud(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("reenvía el enlace a una solicitud sin verificar si pasaron 10 minutos y hay cupo")
    void solicitarAcceso_reenvio() {
        when(usuarios.buscarPorCorreo(CORREO)).thenReturn(Optional.empty());
        when(solicitudes.buscarUltimaRechazada(any(), any())).thenReturn(Optional.empty());
        when(solicitudes.buscarEnCurso(CORREO, "1040123456"))
                .thenReturn(Optional.of(enCurso(EstadoSolicitud.PENDIENTE_VERIFICACION, AHORA.minus(Duration.ofMinutes(11)), 2)));
        when(tokenSeguro.generar()).thenReturn(TOKEN);

        useCase.solicitarAcceso(datos(CORREO));

        ArgumentCaptor<SolicitudAcceso> captor = ArgumentCaptor.forClass(SolicitudAcceso.class);
        verify(solicitudes).actualizar(captor.capture());
        assertThat(captor.getValue().enviosVerificacion()).isEqualTo(3);
        assertThat(captor.getValue().tokenHash()).isEqualTo("hash-token");
        assertThat(captor.getValue().id()).isEqualTo(5L);
        verify(notificaciones).enviarVerificacionSolicitud(CORREO, "Ana Zapata", "valor-token");
    }

    @Test
    @DisplayName("el cupo diario se reinicia al día siguiente")
    void solicitarAcceso_reenvioOtroDia() {
        when(usuarios.buscarPorCorreo(CORREO)).thenReturn(Optional.empty());
        when(solicitudes.buscarUltimaRechazada(any(), any())).thenReturn(Optional.empty());
        when(solicitudes.buscarEnCurso(any(), any()))
                .thenReturn(Optional.of(enCurso(EstadoSolicitud.PENDIENTE_VERIFICACION, AHORA.minus(Duration.ofDays(1)), 3)));
        when(tokenSeguro.generar()).thenReturn(TOKEN);

        useCase.solicitarAcceso(datos(CORREO));

        ArgumentCaptor<SolicitudAcceso> captor = ArgumentCaptor.forClass(SolicitudAcceso.class);
        verify(solicitudes).actualizar(captor.capture());
        assertThat(captor.getValue().enviosVerificacion()).isEqualTo(1);
    }

    @Test
    @DisplayName("no reenvía antes de 10 minutos, sin cupo, si ya está verificada ni si la cédula es de otro correo")
    void solicitarAcceso_sinReenvio() {
        when(usuarios.buscarPorCorreo(CORREO)).thenReturn(Optional.empty());
        when(solicitudes.buscarUltimaRechazada(any(), any())).thenReturn(Optional.empty());
        when(solicitudes.buscarEnCurso(any(), any()))
                .thenReturn(Optional.of(enCurso(EstadoSolicitud.PENDIENTE_VERIFICACION, AHORA.minus(Duration.ofMinutes(5)), 1)))
                .thenReturn(Optional.of(enCurso(EstadoSolicitud.PENDIENTE_VERIFICACION, AHORA.minus(Duration.ofMinutes(30)), 3)))
                .thenReturn(Optional.of(enCurso(EstadoSolicitud.PENDIENTE, AHORA.minus(Duration.ofDays(2)), 1)))
                .thenReturn(Optional.of(enCurso(EstadoSolicitud.PENDIENTE_VERIFICACION, null, 0).withCorreo("otro@udea.edu.co")));

        for (int i = 0; i < 4; i++) {
            useCase.solicitarAcceso(datos(CORREO));
        }

        verify(solicitudes, never()).actualizar(any());
        verifyNoInteractions(notificaciones);
    }

    // ─── verificarCorreo ──────────────────────────────────────────────────────

    @Test
    @DisplayName("verificarCorreo: pasa la solicitud a PENDIENTE y anula el token")
    void verificarCorreo_valido() {
        when(tokenSeguro.hash("valor-token")).thenReturn("hash-token");
        when(solicitudes.buscarPorTokenHash("hash-token")).thenReturn(Optional.of(
                enCurso(EstadoSolicitud.PENDIENTE_VERIFICACION, AHORA, 1).withTokenHash("hash-token")
                        .withTokenExpira(AHORA.plusSeconds(60))));

        useCase.verificarCorreo(" valor-token ");

        ArgumentCaptor<SolicitudAcceso> captor = ArgumentCaptor.forClass(SolicitudAcceso.class);
        verify(solicitudes).actualizar(captor.capture());
        assertThat(captor.getValue().estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(captor.getValue().fechaVerificacion()).isEqualTo(AHORA);
        assertThat(captor.getValue().tokenHash()).isNull();
    }

    @Test
    @DisplayName("verificarCorreo: rechaza tokens vacíos, desconocidos, vencidos o ya usados")
    void verificarCorreo_invalido() {
        when(tokenSeguro.hash(anyString())).thenReturn("h");
        when(solicitudes.buscarPorTokenHash("h"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(enCurso(EstadoSolicitud.PENDIENTE_VERIFICACION, AHORA, 1).withTokenExpira(AHORA.minusSeconds(1))))
                .thenReturn(Optional.of(enCurso(EstadoSolicitud.PENDIENTE, AHORA, 1).withTokenExpira(AHORA.plusSeconds(60))));

        assertThatThrownBy(() -> useCase.verificarCorreo(" ")).isInstanceOf(EnlaceInvalidoException.class);
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> useCase.verificarCorreo("t")).isInstanceOf(EnlaceInvalidoException.class);
        }
        verify(solicitudes, never()).actualizar(any());
    }

    // ─── activarCuenta ────────────────────────────────────────────────────────

    @Test
    @DisplayName("activarCuenta: guarda la contraseña cifrada, activa la cuenta y marca el token como usado")
    void activarCuenta_valida() {
        when(tokenSeguro.hash("valor-token")).thenReturn("hash-token");
        when(tokensCuenta.buscarVigente("hash-token", AHORA))
                .thenReturn(Optional.of(new TokenCuenta(3L, 20L, TokenCuenta.OrigenToken.INVITACION, AHORA.plusSeconds(60), false)));
        when(usuarios.buscarPorId(20L)).thenReturn(Optional.of(Usuario.builder().id(20L).activo(false).passwordHash("{sin-contrasena}").build()));
        when(passwordEncoder.encode("Semilleros2026")).thenReturn("$2a$12$hash");

        useCase.activarCuenta("valor-token", "Semilleros2026");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(captor.capture());
        assertThat(captor.getValue().getActivo()).isTrue();
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$12$hash");
        verify(tokensCuenta).marcarUsado(3L);
    }

    @Test
    @DisplayName("activarCuenta: rechaza enlaces inválidos o de usuarios inexistentes")
    void activarCuenta_enlaceInvalido() {
        when(tokenSeguro.hash(anyString())).thenReturn("h");
        when(tokensCuenta.buscarVigente("h", AHORA))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new TokenCuenta(3L, 99L, TokenCuenta.OrigenToken.APROBACION, AHORA.plusSeconds(60), false)));
        when(usuarios.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.activarCuenta(null, "Semilleros2026")).isInstanceOf(EnlaceInvalidoException.class);
        assertThatThrownBy(() -> useCase.activarCuenta("t", "Semilleros2026")).isInstanceOf(EnlaceInvalidoException.class);
        assertThatThrownBy(() -> useCase.activarCuenta("t", "Semilleros2026")).isInstanceOf(EnlaceInvalidoException.class);
        verify(usuarios, never()).guardar(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"corta1", "sinnumerosaqui", "1234567890123", ""})
    @DisplayName("activarCuenta: exige 10 a 72 caracteres con letras y números")
    void activarCuenta_contrasenaDebil(String contrasena) {
        assertThatThrownBy(() -> useCase.activarCuenta("t", contrasena)).isInstanceOf(SolicitudAccesoInvalidaException.class);
        assertThatThrownBy(() -> useCase.activarCuenta("t", "a1".repeat(37))).isInstanceOf(SolicitudAccesoInvalidaException.class);
        assertThatThrownBy(() -> useCase.activarCuenta("t", null)).isInstanceOf(SolicitudAccesoInvalidaException.class);
        verifyNoInteractions(tokensCuenta);
    }
}
