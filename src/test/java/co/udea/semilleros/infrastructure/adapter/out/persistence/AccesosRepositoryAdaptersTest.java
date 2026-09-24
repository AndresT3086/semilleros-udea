package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.acceso.EstadoSolicitud;
import co.udea.semilleros.domain.model.acceso.SolicitudAcceso;
import co.udea.semilleros.domain.model.acceso.TokenCuenta;
import co.udea.semilleros.domain.model.acceso.TokenGenerado;
import co.udea.semilleros.infrastructure.security.TokenSeguroAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Solicitudes de acceso y tokens de cuenta - SQL sobre H2 (modo PostgreSQL)")
class AccesosRepositoryAdaptersTest {

    private static final Instant AHORA = Instant.parse("2026-09-24T15:00:00Z");

    private DriverManagerDataSource dataSource;
    private SolicitudAccesoRepositoryAdapter solicitudes;
    private TokenCuentaRepositoryAdapter tokens;

    @BeforeEach
    void crearBaseDeDatos() {
        dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:accesos-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("accesos/esquema.sql")).execute(dataSource);
        NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(dataSource);
        solicitudes = new SolicitudAccesoRepositoryAdapter(jdbc);
        tokens = new TokenCuentaRepositoryAdapter(jdbc);
    }

    @AfterEach
    void cerrarBaseDeDatos() {
        new NamedParameterJdbcTemplate(dataSource).getJdbcTemplate().execute("SHUTDOWN");
    }

    private static SolicitudAcceso solicitud(String correo, String cedula, EstadoSolicitud estado, String token) {
        return SolicitudAcceso.builder().nombres("Ana").apellidos("Zapata").cedula(cedula).correo(correo)
                .idUnidadAcademica(1L).justificacion("Coordino un semillero").estado(estado).tokenHash(token)
                .tokenExpira(AHORA.plus(Duration.ofHours(1))).enviosVerificacion(1).ultimoEnvio(AHORA)
                .ipOrigen("10.0.0.1").fechaCreacion(AHORA).build();
    }

    @Test
    @DisplayName("crear y buscar: guarda todos los datos, con el nombre de la unidad, y busca por id y token")
    void crearYBuscar() {
        Long id = solicitudes.crear(solicitud("ana@udea.edu.co", "1040123456", EstadoSolicitud.PENDIENTE_VERIFICACION, "h1"));

        SolicitudAcceso guardada = solicitudes.buscarPorId(id).orElseThrow();
        assertThat(guardada.correo()).isEqualTo("ana@udea.edu.co");
        assertThat(guardada.unidadAcademica()).isEqualTo("Facultad de Ingeniería");
        assertThat(guardada.tokenExpira()).isEqualTo(AHORA.plus(Duration.ofHours(1)));
        assertThat(guardada.fechaVerificacion()).isNull();
        assertThat(solicitudes.buscarPorTokenHash("h1")).map(SolicitudAcceso::id).contains(id);
        assertThat(solicitudes.buscarPorTokenHash("otro")).isEmpty();
    }

    @Test
    @DisplayName("crear: retorna null si choca con un registro existente (token duplicado)")
    void crear_duplicado() {
        solicitudes.crear(solicitud("ana@udea.edu.co", "1040123456", EstadoSolicitud.PENDIENTE_VERIFICACION, "h1"));

        assertThat(solicitudes.crear(solicitud("beto@udea.edu.co", "1040999999", EstadoSolicitud.PENDIENTE_VERIFICACION, "h1")))
                .isNull();
    }

    @Test
    @DisplayName("buscarEnCurso: por correo o por cédula, prefiriendo el correo; ignora las cerradas")
    void buscarEnCurso() {
        solicitudes.crear(solicitud("otro@udea.edu.co", "1040123456", EstadoSolicitud.PENDIENTE, "h1"));
        solicitudes.crear(solicitud("ana@udea.edu.co", "1111111111", EstadoSolicitud.PENDIENTE_VERIFICACION, "h2"));
        solicitudes.crear(solicitud("cerrada@udea.edu.co", "2222222222", EstadoSolicitud.APROBADA, "h3"));

        assertThat(solicitudes.buscarEnCurso("ana@udea.edu.co", "1040123456")).map(SolicitudAcceso::correo)
                .contains("ana@udea.edu.co");
        assertThat(solicitudes.buscarEnCurso("nadie@udea.edu.co", "1040123456")).map(SolicitudAcceso::correo)
                .contains("otro@udea.edu.co");
        assertThat(solicitudes.buscarEnCurso("nadie@udea.edu.co", null)).isEmpty();
        assertThat(solicitudes.buscarEnCurso("cerrada@udea.edu.co", "2222222222")).isEmpty();
    }

    @Test
    @DisplayName("actualizar y buscarUltimaRechazada: el bloqueo prevalece sobre rechazos posteriores")
    void actualizarYRechazadas() {
        Long bloqueada = solicitudes.crear(solicitud("ana@udea.edu.co", "1040123456", EstadoSolicitud.PENDIENTE, "h1"));
        Long reciente = solicitudes.crear(solicitud("ana@udea.edu.co", "1040123456", EstadoSolicitud.PENDIENTE, "h2"));
        solicitudes.actualizar(solicitudes.buscarPorId(bloqueada).orElseThrow().toBuilder()
                .estado(EstadoSolicitud.RECHAZADA).bloqueada(true).motivoRechazo("Spam").idRevisor(6L)
                .fechaRevision(AHORA.minus(Duration.ofDays(90))).tokenHash(null).build());
        solicitudes.actualizar(solicitudes.buscarPorId(reciente).orElseThrow().toBuilder()
                .estado(EstadoSolicitud.RECHAZADA).fechaRevision(AHORA).fechaVerificacion(AHORA).build());

        SolicitudAcceso ultima = solicitudes.buscarUltimaRechazada("ana@udea.edu.co", null).orElseThrow();
        assertThat(ultima.id()).isEqualTo(bloqueada);
        assertThat(ultima.bloqueada()).isTrue();
        assertThat(ultima.motivoRechazo()).isEqualTo("Spam");
        assertThat(ultima.idRevisor()).isEqualTo(6L);
        assertThat(ultima.tokenHash()).isNull();
        assertThat(solicitudes.buscarUltimaRechazada("x@udea.edu.co", "1040123456")).isPresent();
    }

    @Test
    @DisplayName("listarPorEstado, contarPorEstado y limpieza de no verificadas vencidas")
    void listarContarYLimpiar() {
        solicitudes.crear(solicitud("a@udea.edu.co", "1000000001", EstadoSolicitud.PENDIENTE, null));
        solicitudes.crear(solicitud("b@udea.edu.co", "1000000002", EstadoSolicitud.PENDIENTE, null));
        solicitudes.crear(solicitud("c@udea.edu.co", "1000000003", EstadoSolicitud.PENDIENTE_VERIFICACION, "h3"));

        assertThat(solicitudes.listarPorEstado(EstadoSolicitud.PENDIENTE)).extracting(SolicitudAcceso::correo)
                .containsExactly("a@udea.edu.co", "b@udea.edu.co");
        assertThat(solicitudes.contarPorEstado(EstadoSolicitud.PENDIENTE)).isEqualTo(2);
        assertThat(solicitudes.eliminarNoVerificadasVencidas(AHORA)).isZero();
        assertThat(solicitudes.eliminarNoVerificadasVencidas(AHORA.plus(Duration.ofHours(2)))).isEqualTo(1);
        assertThat(solicitudes.contarPorEstado(EstadoSolicitud.PENDIENTE_VERIFICACION)).isZero();
    }

    @Test
    @DisplayName("tokens de cuenta: uno vigente por usuario, se marca como usado y se limpian los viejos")
    void tokensDeCuenta() {
        Instant expira = AHORA.plus(Duration.ofHours(24));
        tokens.crear(20L, "t1", TokenCuenta.OrigenToken.INVITACION, expira);
        tokens.crear(20L, "t2", TokenCuenta.OrigenToken.INVITACION, expira);

        assertThat(tokens.buscarVigente("t1", AHORA)).as("el reenvío invalida el enlace anterior").isEmpty();
        TokenCuenta vigente = tokens.buscarVigente("t2", AHORA).orElseThrow();
        assertThat(vigente.idUsuario()).isEqualTo(20L);
        assertThat(vigente.origen()).isEqualTo(TokenCuenta.OrigenToken.INVITACION);
        assertThat(vigente.expira()).isEqualTo(expira);
        assertThat(tokens.buscarVigente("t2", expira.plusSeconds(1))).as("vencido").isEmpty();

        tokens.marcarUsado(vigente.id());
        assertThat(tokens.buscarVigente("t2", AHORA)).isEmpty();
        assertThat(tokens.eliminarAnterioresA(expira.plus(1, ChronoUnit.DAYS))).isEqualTo(2);
    }

    @Test
    @DisplayName("TokenSeguroAdapter: tokens aleatorios de 256 bits para URL y hash SHA-256 estable")
    void tokenSeguro() {
        TokenSeguroAdapter adapter = new TokenSeguroAdapter();
        TokenGenerado uno = adapter.generar();
        TokenGenerado dos = adapter.generar();

        assertThat(uno.valor()).hasSize(43).matches("[A-Za-z0-9_-]+").isNotEqualTo(dos.valor());
        assertThat(uno.hash()).hasSize(64).isEqualTo(adapter.hash(uno.valor()));
        assertThat(adapter.hash("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(uno.toString()).doesNotContain(uno.valor());
    }
}
