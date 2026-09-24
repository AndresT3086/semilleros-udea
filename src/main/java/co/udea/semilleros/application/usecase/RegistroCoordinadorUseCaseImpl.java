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
import co.udea.semilleros.domain.port.in.RegistroCoordinadorUseCase;
import co.udea.semilleros.domain.port.out.NotificacionEmailPort;
import co.udea.semilleros.domain.port.out.SolicitudAccesoRepositoryPort;
import co.udea.semilleros.domain.port.out.TokenCuentaRepositoryPort;
import co.udea.semilleros.domain.port.out.TokenSeguroPort;
import co.udea.semilleros.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

/**
 * Registro de coordinadores. Capas contra el abuso del formulario público:
 * dominio institucional, captcha, campo trampa, límite por IP (en el filtro HTTP),
 * una solicitud en curso por correo y cédula, espera entre reenvíos y máximo diario,
 * espera tras un rechazo y verificación del correo antes de llegar al administrador.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RegistroCoordinadorUseCaseImpl implements RegistroCoordinadorUseCase {

    static final int LONGITUD_MINIMA_CONTRASENA = 10;
    static final int LONGITUD_MAXIMA_CONTRASENA = 72;

    private final SolicitudAccesoRepositoryPort solicitudAccesoRepositoryPort;
    private final TokenCuentaRepositoryPort tokenCuentaRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final TokenSeguroPort tokenSeguroPort;
    private final NotificacionEmailPort notificacionEmailPort;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Value("${app.security.allowed-email-domain}")
    private String dominioPermitido;

    @Value("${app.accesos.verificacion-horas:1}")
    private long horasVerificacion;

    @Value("${app.accesos.minutos-entre-envios:10}")
    private long minutosEntreEnvios;

    @Value("${app.accesos.max-envios-dia:3}")
    private int maxEnviosDia;

    @Value("${app.accesos.espera-rechazo-dias:30}")
    private long diasEsperaRechazo;

    @Override
    public void solicitarAcceso(DatosSolicitud datos) {
        if (datos.sitioWeb() != null && !datos.sitioWeb().isBlank()) {
            log.info("Solicitud de acceso descartada: se llenó el campo trampa");
            return;
        }
        String correo = normalizarCorreo(datos.correo());
        if (!correo.endsWith(dominioPermitido)) {
            throw new DominioCorreoNoPermitidoException(correo);
        }
        if (datos.respuestaMath() != datos.operando1() + datos.operando2()) {
            throw new ValidacionBotException();
        }

        Instant ahora = clock.instant();
        if (usuarioRepositoryPort.buscarPorCorreo(correo).isPresent()) {
            return;
        }
        boolean rechazoVigente = solicitudAccesoRepositoryPort.buscarUltimaRechazada(correo, datos.cedula())
                .filter(rechazada -> rechazada.bloqueada()
                        || rechazada.fechaRevision().plus(Duration.ofDays(diasEsperaRechazo)).isAfter(ahora))
                .isPresent();
        if (rechazoVigente) {
            return;
        }

        Optional<SolicitudAcceso> enCurso = solicitudAccesoRepositoryPort.buscarEnCurso(correo, datos.cedula());
        if (enCurso.isEmpty()) {
            crearSolicitud(datos, correo, ahora);
            return;
        }
        SolicitudAcceso actual = enCurso.get();
        // Otra persona con la misma cédula, o solicitud ya confirmada: no se toca
        if (!actual.correo().equals(correo) || actual.estado() != EstadoSolicitud.PENDIENTE_VERIFICACION
                || !puedeReenviar(actual, ahora)) {
            return;
        }
        int envios = mismoDia(actual.ultimoEnvio(), ahora) ? actual.enviosVerificacion() + 1 : 1;
        TokenGenerado token = tokenSeguroPort.generar();
        solicitudAccesoRepositoryPort.actualizar(actual.toBuilder()
                .nombres(datos.nombres().trim())
                .apellidos(datos.apellidos().trim())
                .cedula(datos.cedula())
                .idUnidadAcademica(datos.idUnidadAcademica())
                .justificacion(datos.justificacion().trim())
                .tokenHash(token.hash())
                .tokenExpira(ahora.plus(Duration.ofHours(horasVerificacion)))
                .enviosVerificacion(envios)
                .ultimoEnvio(ahora)
                .ipOrigen(datos.ip())
                .build());
        notificacionEmailPort.enviarVerificacionSolicitud(correo, actual.nombreCompleto(), token.valor());
    }

    private void crearSolicitud(DatosSolicitud datos, String correo, Instant ahora) {
        TokenGenerado token = tokenSeguroPort.generar();
        SolicitudAcceso nueva = SolicitudAcceso.builder()
                .nombres(datos.nombres().trim())
                .apellidos(datos.apellidos().trim())
                .cedula(datos.cedula())
                .correo(correo)
                .idUnidadAcademica(datos.idUnidadAcademica())
                .justificacion(datos.justificacion().trim())
                .estado(EstadoSolicitud.PENDIENTE_VERIFICACION)
                .tokenHash(token.hash())
                .tokenExpira(ahora.plus(Duration.ofHours(horasVerificacion)))
                .enviosVerificacion(1)
                .ultimoEnvio(ahora)
                .ipOrigen(datos.ip())
                .fechaCreacion(ahora)
                .build();
        // null: otra petición simultánea registró la misma solicitud
        if (solicitudAccesoRepositoryPort.crear(nueva) != null) {
            notificacionEmailPort.enviarVerificacionSolicitud(correo, nueva.nombreCompleto(), token.valor());
        }
    }

    private boolean puedeReenviar(SolicitudAcceso solicitud, Instant ahora) {
        Instant ultimo = solicitud.ultimoEnvio();
        if (ultimo == null) {
            return true;
        }
        boolean esperoSuficiente = !ultimo.plus(Duration.ofMinutes(minutosEntreEnvios)).isAfter(ahora);
        boolean dentroDelCupo = !mismoDia(ultimo, ahora) || solicitud.enviosVerificacion() < maxEnviosDia;
        return esperoSuficiente && dentroDelCupo;
    }

    private boolean mismoDia(Instant instante, Instant ahora) {
        return instante != null
                && LocalDate.ofInstant(instante, clock.getZone()).equals(LocalDate.ofInstant(ahora, clock.getZone()));
    }

    @Override
    public void verificarCorreo(String token) {
        Instant ahora = clock.instant();
        SolicitudAcceso solicitud = buscarPorToken(token)
                .filter(s -> s.estado() == EstadoSolicitud.PENDIENTE_VERIFICACION)
                .filter(s -> s.tokenExpira() != null && s.tokenExpira().isAfter(ahora))
                .orElseThrow(EnlaceInvalidoException::new);
        solicitudAccesoRepositoryPort.actualizar(solicitud.toBuilder()
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaVerificacion(ahora)
                .tokenHash(null)
                .tokenExpira(null)
                .build());
    }

    private Optional<SolicitudAcceso> buscarPorToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return solicitudAccesoRepositoryPort.buscarPorTokenHash(tokenSeguroPort.hash(token.trim()));
    }

    @Override
    public void activarCuenta(String token, String contrasena) {
        validarContrasena(contrasena);
        if (token == null || token.isBlank()) {
            throw new EnlaceInvalidoException();
        }
        TokenCuenta vigente = tokenCuentaRepositoryPort
                .buscarVigente(tokenSeguroPort.hash(token.trim()), clock.instant())
                .orElseThrow(EnlaceInvalidoException::new);
        Usuario usuario = usuarioRepositoryPort.buscarPorId(vigente.idUsuario())
                .orElseThrow(EnlaceInvalidoException::new);
        usuarioRepositoryPort.guardar(usuario
                .withPasswordHash(passwordEncoder.encode(contrasena))
                .withActivo(true));
        tokenCuentaRepositoryPort.marcarUsado(vigente.id());
    }

    static void validarContrasena(String contrasena) {
        if (contrasena == null
                || contrasena.length() < LONGITUD_MINIMA_CONTRASENA
                || contrasena.length() > LONGITUD_MAXIMA_CONTRASENA
                || contrasena.chars().noneMatch(Character::isLetter)
                || contrasena.chars().noneMatch(Character::isDigit)) {
            throw new SolicitudAccesoInvalidaException("La contraseña debe tener entre "
                    + LONGITUD_MINIMA_CONTRASENA + " y " + LONGITUD_MAXIMA_CONTRASENA
                    + " caracteres e incluir al menos una letra y un número.");
        }
    }

    static String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }
}
