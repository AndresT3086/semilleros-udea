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
import co.udea.semilleros.domain.port.in.AdministrarAccesosUseCase;
import co.udea.semilleros.domain.port.out.NotificacionEmailPort;
import co.udea.semilleros.domain.port.out.SolicitudAccesoRepositoryPort;
import co.udea.semilleros.domain.port.out.TokenCuentaRepositoryPort;
import co.udea.semilleros.domain.port.out.TokenSeguroPort;
import co.udea.semilleros.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdministrarAccesosUseCaseImpl implements AdministrarAccesosUseCase {

    static final String ROL_COORDINADOR = "COORDINADOR";
    /** Valor que nunca coincide con un hash BCrypt: la cuenta no tiene contraseña hasta activarse. */
    static final String SIN_CONTRASENA = "{sin-contrasena}";
    static final int LONGITUD_MAXIMA_MOTIVO = 500;
    private static final Duration RETENCION_TOKENS = Duration.ofDays(7);

    private final SolicitudAccesoRepositoryPort solicitudAccesoRepositoryPort;
    private final TokenCuentaRepositoryPort tokenCuentaRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final TokenSeguroPort tokenSeguroPort;
    private final NotificacionEmailPort notificacionEmailPort;
    private final Clock clock;

    @Value("${app.security.allowed-email-domain}")
    private String dominioPermitido;

    @Value("${app.accesos.activacion-horas:24}")
    private long horasActivacion;

    @Value("${app.admin.correo:admin@udea.edu.co}")
    private String correoAdministrador;

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudAcceso> listarSolicitudes(EstadoSolicitud estado) {
        return solicitudAccesoRepositoryPort.listarPorEstado(estado == null ? EstadoSolicitud.PENDIENTE : estado);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarPendientes() {
        return solicitudAccesoRepositoryPort.contarPorEstado(EstadoSolicitud.PENDIENTE);
    }

    @Override
    public boolean aprobar(Long idSolicitud, Long idAdministrador) {
        SolicitudAcceso solicitud = solicitudPendiente(idSolicitud);
        if (usuarioRepositoryPort.buscarPorCorreo(solicitud.correo()).isPresent()) {
            throw new ConflictoAccesoException("Ya existe una cuenta con el correo de esta solicitud.");
        }
        Usuario usuario = usuarioRepositoryPort.guardar(nuevoCoordinador(
                solicitud.nombres(), solicitud.apellidos(), solicitud.correo()));
        boolean correoEnviado = enviarActivacion(usuario, TokenCuenta.OrigenToken.APROBACION).correoEnviado();
        solicitudAccesoRepositoryPort.actualizar(solicitud.toBuilder()
                .estado(EstadoSolicitud.APROBADA)
                .idRevisor(idAdministrador)
                .fechaRevision(clock.instant())
                .build());
        return correoEnviado;
    }

    @Override
    public void rechazar(Long idSolicitud, Long idAdministrador, String motivo, boolean bloquear) {
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.isEmpty() || motivoLimpio.length() > LONGITUD_MAXIMA_MOTIVO) {
            throw new SolicitudAccesoInvalidaException(
                    "Indica el motivo del rechazo (máximo " + LONGITUD_MAXIMA_MOTIVO + " caracteres).");
        }
        SolicitudAcceso solicitud = solicitudPendiente(idSolicitud);
        solicitudAccesoRepositoryPort.actualizar(solicitud.toBuilder()
                .estado(EstadoSolicitud.RECHAZADA)
                .motivoRechazo(motivoLimpio)
                .bloqueada(bloquear)
                .idRevisor(idAdministrador)
                .fechaRevision(clock.instant())
                .build());
        notificacionEmailPort.notificarRechazoSolicitud(solicitud.correo(), solicitud.nombreCompleto(), motivoLimpio);
    }

    /**
     * Invita a un coordinador. Si la cuenta existe pero aún no se activó, reenvía el enlace
     * (el anterior deja de servir). Cierra como aprobada una solicitud en curso del mismo correo.
     */
    @Override
    public InvitacionEnviada invitar(DatosInvitacion datos, Long idAdministrador) {
        String correo = RegistroCoordinadorUseCaseImpl.normalizarCorreo(datos.correo());
        if (!correo.endsWith(dominioPermitido) || correo.length() <= dominioPermitido.length()) {
            throw new DominioCorreoNoPermitidoException(correo);
        }
        if (vacio(datos.nombres()) || vacio(datos.apellidos())) {
            throw new SolicitudAccesoInvalidaException("Los nombres y apellidos de la persona invitada son obligatorios.");
        }
        Optional<Usuario> existente = usuarioRepositoryPort.buscarPorCorreo(correo);
        if (existente.filter(Usuario::getActivo).isPresent()) {
            throw new ConflictoAccesoException("Ya existe una cuenta activa con ese correo.");
        }
        Usuario usuario = existente.orElseGet(() -> usuarioRepositoryPort.guardar(
                nuevoCoordinador(datos.nombres().trim(), datos.apellidos().trim(), correo)));
        EnvioActivacion envio = enviarActivacion(usuario, TokenCuenta.OrigenToken.INVITACION);

        solicitudAccesoRepositoryPort.buscarEnCurso(correo, null)
                .filter(solicitud -> solicitud.correo().equals(correo))
                .ifPresent(solicitud -> solicitudAccesoRepositoryPort.actualizar(solicitud.toBuilder()
                        .estado(EstadoSolicitud.APROBADA)
                        .tokenHash(null)
                        .tokenExpira(null)
                        .idRevisor(idAdministrador)
                        .fechaRevision(clock.instant())
                        .build()));
        return new InvitacionEnviada(usuario.getId(), correo, envio.expira(), existente.isPresent(), envio.correoEnviado());
    }

    @Override
    public int limpiarVencidos() {
        Instant ahora = clock.instant();
        return solicitudAccesoRepositoryPort.eliminarNoVerificadasVencidas(ahora)
                + tokenCuentaRepositoryPort.eliminarAnterioresA(ahora.minus(RETENCION_TOKENS));
    }

    @Override
    @Transactional(readOnly = true)
    public void enviarResumenPendientes() {
        long pendientes = contarPendientes();
        if (pendientes > 0) {
            notificacionEmailPort.enviarResumenSolicitudesPendientes(correoAdministrador, pendientes);
        }
    }

    private SolicitudAcceso solicitudPendiente(Long idSolicitud) {
        SolicitudAcceso solicitud = solicitudAccesoRepositoryPort.buscarPorId(idSolicitud)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de acceso", idSolicitud));
        if (solicitud.estado() != EstadoSolicitud.PENDIENTE) {
            throw new ConflictoAccesoException("La solicitud ya fue revisada o su correo aún no se ha confirmado.");
        }
        return solicitud;
    }

    private static Usuario nuevoCoordinador(String nombres, String apellidos, String correo) {
        return Usuario.builder()
                .nombres(nombres)
                .apellidos(apellidos)
                .correo(correo)
                .passwordHash(SIN_CONTRASENA)
                .rol(ROL_COORDINADOR)
                .activo(false)
                .build();
    }

    private record EnvioActivacion(Instant expira, boolean correoEnviado) {
    }

    private EnvioActivacion enviarActivacion(Usuario usuario, TokenCuenta.OrigenToken origen) {
        TokenGenerado token = tokenSeguroPort.generar();
        Instant expira = clock.instant().plus(Duration.ofHours(horasActivacion));
        tokenCuentaRepositoryPort.crear(usuario.getId(), token.hash(), origen, expira);
        boolean correoEnviado = notificacionEmailPort.enviarActivacionCuenta(usuario.getCorreo(),
                (usuario.getNombres() + " " + usuario.getApellidos()).trim(), token.valor(),
                origen == TokenCuenta.OrigenToken.INVITACION);
        return new EnvioActivacion(expira, correoEnviado);
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
