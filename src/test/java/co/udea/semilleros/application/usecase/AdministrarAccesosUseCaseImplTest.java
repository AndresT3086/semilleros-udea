package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.ConflictoAccesoException;
import co.udea.semilleros.domain.exception.DominioCorreoNoPermitidoException;
import co.udea.semilleros.domain.exception.RecursoNoEncontradoException;
import co.udea.semilleros.domain.exception.SolicitudAccesoInvalidaException;
import co.udea.semilleros.domain.model.Usuario;
import co.udea.semilleros.domain.model.acceso.DatosInvitacion;
import co.udea.semilleros.domain.model.acceso.EstadoSolicitud;
import co.udea.semilleros.domain.model.acceso.InvitacionEnviada;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdministrarAccesosUseCaseImpl - Aprobación, rechazo e invitaciones")
class AdministrarAccesosUseCaseImplTest {

    private static final Instant AHORA = Instant.parse("2026-09-24T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneId.of("America/Bogota"));
    private static final Long ADMIN = 6L;

    @Mock private SolicitudAccesoRepositoryPort solicitudes;
    @Mock private TokenCuentaRepositoryPort tokensCuenta;
    @Mock private UsuarioRepositoryPort usuarios;
    @Mock private TokenSeguroPort tokenSeguro;
    @Mock private NotificacionEmailPort notificaciones;

    private AdministrarAccesosUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new AdministrarAccesosUseCaseImpl(solicitudes, tokensCuenta, usuarios, tokenSeguro, notificaciones, CLOCK);
        ReflectionTestUtils.setField(useCase, "dominioPermitido", "@udea.edu.co");
        ReflectionTestUtils.setField(useCase, "horasActivacion", 24L);
        ReflectionTestUtils.setField(useCase, "correoAdministrador", "admin@udea.edu.co");
    }

    private static SolicitudAcceso solicitud(EstadoSolicitud estado) {
        return SolicitudAcceso.builder().id(5L).nombres("Ana").apellidos("Zapata").cedula("1040123456")
                .correo("ana@udea.edu.co").justificacion("x").estado(estado).build();
    }

    @Test
    @DisplayName("listarSolicitudes y contarPendientes: por defecto las pendientes de revisión")
    void listarYContar() {
        List<SolicitudAcceso> pendientes = List.of(solicitud(EstadoSolicitud.PENDIENTE));
        when(solicitudes.listarPorEstado(EstadoSolicitud.PENDIENTE)).thenReturn(pendientes);
        when(solicitudes.contarPorEstado(EstadoSolicitud.PENDIENTE)).thenReturn(4L);

        assertThat(useCase.listarSolicitudes(null)).isEqualTo(pendientes);
        assertThat(useCase.contarPendientes()).isEqualTo(4);
    }

    @Test
    @DisplayName("aprobar: crea el coordinador inactivo sin contraseña, envía el enlace de 24 h y registra la revisión")
    void aprobar_creaUsuarioYEnlace() {
        when(solicitudes.buscarPorId(5L)).thenReturn(Optional.of(solicitud(EstadoSolicitud.PENDIENTE)));
        when(usuarios.buscarPorCorreo("ana@udea.edu.co")).thenReturn(Optional.empty());
        when(usuarios.guardar(any())).thenAnswer(inv -> ((Usuario) inv.getArgument(0)).withId(20L));
        when(tokenSeguro.generar()).thenReturn(new TokenGenerado("valor", "hash"));

        useCase.aprobar(5L, ADMIN);

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(usuario.capture());
        assertThat(usuario.getValue().getRol()).isEqualTo("COORDINADOR");
        assertThat(usuario.getValue().getActivo()).isFalse();
        assertThat(usuario.getValue().getPasswordHash()).isEqualTo(AdministrarAccesosUseCaseImpl.SIN_CONTRASENA);
        verify(tokensCuenta).crear(20L, "hash", TokenCuenta.OrigenToken.APROBACION, AHORA.plus(Duration.ofHours(24)));
        verify(notificaciones).enviarActivacionCuenta("ana@udea.edu.co", "Ana Zapata", "valor", false);
        ArgumentCaptor<SolicitudAcceso> revisada = ArgumentCaptor.forClass(SolicitudAcceso.class);
        verify(solicitudes).actualizar(revisada.capture());
        assertThat(revisada.getValue().estado()).isEqualTo(EstadoSolicitud.APROBADA);
        assertThat(revisada.getValue().idRevisor()).isEqualTo(ADMIN);
        assertThat(revisada.getValue().fechaRevision()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("aprobar: solo solicitudes pendientes, existentes y sin cuenta previa")
    void aprobar_validaciones() {
        when(solicitudes.buscarPorId(1L)).thenReturn(Optional.empty());
        when(solicitudes.buscarPorId(2L)).thenReturn(Optional.of(solicitud(EstadoSolicitud.PENDIENTE_VERIFICACION)));
        when(solicitudes.buscarPorId(3L)).thenReturn(Optional.of(solicitud(EstadoSolicitud.PENDIENTE)));
        when(usuarios.buscarPorCorreo("ana@udea.edu.co")).thenReturn(Optional.of(Usuario.builder().id(1L).build()));

        assertThatThrownBy(() -> useCase.aprobar(1L, ADMIN)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> useCase.aprobar(2L, ADMIN)).isInstanceOf(ConflictoAccesoException.class);
        assertThatThrownBy(() -> useCase.aprobar(3L, ADMIN)).isInstanceOf(ConflictoAccesoException.class);
        verify(usuarios, never()).guardar(any());
        verifyNoInteractions(tokensCuenta, notificaciones);
    }

    @Test
    @DisplayName("rechazar: exige motivo, registra el bloqueo y notifica a la persona")
    void rechazar() {
        when(solicitudes.buscarPorId(5L)).thenReturn(Optional.of(solicitud(EstadoSolicitud.PENDIENTE)));

        assertThatThrownBy(() -> useCase.rechazar(5L, ADMIN, "  ", false)).isInstanceOf(SolicitudAccesoInvalidaException.class);
        assertThatThrownBy(() -> useCase.rechazar(5L, ADMIN, "x".repeat(501), false)).isInstanceOf(SolicitudAccesoInvalidaException.class);
        useCase.rechazar(5L, ADMIN, " No coordina ningún semillero ", true);

        ArgumentCaptor<SolicitudAcceso> captor = ArgumentCaptor.forClass(SolicitudAcceso.class);
        verify(solicitudes).actualizar(captor.capture());
        assertThat(captor.getValue().estado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(captor.getValue().motivoRechazo()).isEqualTo("No coordina ningún semillero");
        assertThat(captor.getValue().bloqueada()).isTrue();
        verify(notificaciones).notificarRechazoSolicitud("ana@udea.edu.co", "Ana Zapata", "No coordina ningún semillero");
    }

    @Test
    @DisplayName("invitar: solo correos @udea.edu.co y con nombres")
    void invitar_validaciones() {
        assertThatThrownBy(() -> useCase.invitar(new DatosInvitacion("Ana", "Zapata", "ana@gmail.com"), ADMIN))
                .isInstanceOf(DominioCorreoNoPermitidoException.class);
        assertThatThrownBy(() -> useCase.invitar(new DatosInvitacion("Ana", "Zapata", "@udea.edu.co"), ADMIN))
                .isInstanceOf(DominioCorreoNoPermitidoException.class);
        assertThatThrownBy(() -> useCase.invitar(new DatosInvitacion("Ana", "Zapata", "ana@udea.edu.co.evil.com"), ADMIN))
                .isInstanceOf(DominioCorreoNoPermitidoException.class);
        assertThatThrownBy(() -> useCase.invitar(new DatosInvitacion(" ", "Zapata", "ana@udea.edu.co"), ADMIN))
                .isInstanceOf(SolicitudAccesoInvalidaException.class);
        verifyNoInteractions(usuarios);
    }

    @Test
    @DisplayName("invitar: crea la cuenta inactiva, envía el enlace y cierra la solicitud en curso del mismo correo")
    void invitar_nuevo() {
        when(usuarios.buscarPorCorreo("ana@udea.edu.co")).thenReturn(Optional.empty());
        when(usuarios.guardar(any())).thenAnswer(inv -> ((Usuario) inv.getArgument(0)).withId(21L));
        when(tokenSeguro.generar()).thenReturn(new TokenGenerado("valor", "hash"));
        when(solicitudes.buscarEnCurso(eq("ana@udea.edu.co"), isNull()))
                .thenReturn(Optional.of(solicitud(EstadoSolicitud.PENDIENTE_VERIFICACION)));

        InvitacionEnviada resultado = useCase.invitar(new DatosInvitacion(" Ana ", " Zapata ", " ANA@udea.edu.co "), ADMIN);

        assertThat(resultado).isEqualTo(new InvitacionEnviada(21L, "ana@udea.edu.co", AHORA.plus(Duration.ofHours(24)), false));
        verify(tokensCuenta).crear(21L, "hash", TokenCuenta.OrigenToken.INVITACION, AHORA.plus(Duration.ofHours(24)));
        verify(notificaciones).enviarActivacionCuenta("ana@udea.edu.co", "Ana Zapata", "valor", true);
        ArgumentCaptor<SolicitudAcceso> captor = ArgumentCaptor.forClass(SolicitudAcceso.class);
        verify(solicitudes).actualizar(captor.capture());
        assertThat(captor.getValue().estado()).isEqualTo(EstadoSolicitud.APROBADA);
        assertThat(captor.getValue().tokenHash()).isNull();
    }

    @Test
    @DisplayName("invitar: a una cuenta sin activar le reenvía el enlace; a una activa la rechaza")
    void invitar_existente() {
        Usuario pendiente = Usuario.builder().id(22L).nombres("Ana").apellidos("Zapata").correo("ana@udea.edu.co").activo(false).build();
        when(usuarios.buscarPorCorreo("ana@udea.edu.co"))
                .thenReturn(Optional.of(pendiente))
                .thenReturn(Optional.of(pendiente.withActivo(true)));
        when(tokenSeguro.generar()).thenReturn(new TokenGenerado("valor", "hash"));
        when(solicitudes.buscarEnCurso(any(), isNull())).thenReturn(Optional.empty());

        InvitacionEnviada reenviada = useCase.invitar(new DatosInvitacion("Ana", "Zapata", "ana@udea.edu.co"), ADMIN);

        assertThat(reenviada.reenviada()).isTrue();
        verify(usuarios, never()).guardar(any());
        assertThatThrownBy(() -> useCase.invitar(new DatosInvitacion("Ana", "Zapata", "ana@udea.edu.co"), ADMIN))
                .isInstanceOf(ConflictoAccesoException.class);
    }

    @Test
    @DisplayName("limpiarVencidos: borra solicitudes sin verificar vencidas y tokens de más de 7 días")
    void limpiarVencidos() {
        when(solicitudes.eliminarNoVerificadasVencidas(AHORA)).thenReturn(3);
        when(tokensCuenta.eliminarAnterioresA(AHORA.minus(Duration.ofDays(7)))).thenReturn(2);

        assertThat(useCase.limpiarVencidos()).isEqualTo(5);
    }

    @Test
    @DisplayName("enviarResumenPendientes: avisa al administrador solo si hay pendientes")
    void enviarResumenPendientes() {
        when(solicitudes.contarPorEstado(EstadoSolicitud.PENDIENTE)).thenReturn(0L).thenReturn(2L);

        useCase.enviarResumenPendientes();
        useCase.enviarResumenPendientes();

        verify(notificaciones).enviarResumenSolicitudesPendientes("admin@udea.edu.co", 2L);
        verify(notificaciones, never()).enviarResumenSolicitudesPendientes(any(), eq(0L));
        verify(tokensCuenta, never()).crear(anyLong(), any(), any(), any());
    }
}
