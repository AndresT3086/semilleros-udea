package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.acceso.EstadoSolicitud;
import co.udea.semilleros.domain.model.acceso.SolicitudAcceso;
import co.udea.semilleros.domain.port.out.SolicitudAccesoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SolicitudAccesoRepositoryAdapter implements SolicitudAccesoRepositoryPort {

    private static final String SELECT = """
            SELECT sa.*, ua.nombre AS unidad_nombre
            FROM solicitud_acceso sa
            LEFT JOIN unidad_academica ua ON ua.id_unidad = sa.id_unidad_academica
            """;

    /** Prioriza la coincidencia por correo sobre la coincidencia por cédula. */
    private static final String EN_CURSO = SELECT + """
            WHERE sa.estado IN ('PENDIENTE_VERIFICACION', 'PENDIENTE')
              AND (sa.correo = :correo OR (:cedula IS NOT NULL AND sa.cedula = :cedula))
            ORDER BY CASE WHEN sa.correo = :correo THEN 0 ELSE 1 END, sa.id_solicitud
            LIMIT 1
            """;

    /** Las bloqueadas primero: un bloqueo vale aunque haya rechazos posteriores. */
    private static final String ULTIMA_RECHAZADA = SELECT + """
            WHERE sa.estado = 'RECHAZADA'
              AND (sa.correo = :correo OR (:cedula IS NOT NULL AND sa.cedula = :cedula))
            ORDER BY sa.bloqueada DESC, sa.fecha_revision DESC
            LIMIT 1
            """;

    private static final String POR_ID = SELECT + " WHERE sa.id_solicitud = :id";

    private static final String POR_TOKEN = SELECT + " WHERE sa.token_hash = :token";

    private static final String POR_ESTADO = SELECT
            + " WHERE sa.estado = :estado ORDER BY COALESCE(sa.fecha_verificacion, sa.fecha_creacion), sa.id_solicitud";

    private static final String CONTAR_POR_ESTADO = "SELECT COUNT(*) FROM solicitud_acceso WHERE estado = :estado";

    private static final String INSERTAR = """
            INSERT INTO solicitud_acceso (nombres, apellidos, cedula, correo, id_unidad_academica, justificacion,
                estado, token_hash, token_expira, envios_verificacion, ultimo_envio, ip_origen, fecha_creacion)
            VALUES (:nombres, :apellidos, :cedula, :correo, :idUnidad, :justificacion,
                :estado, :tokenHash, :tokenExpira, :envios, :ultimoEnvio, :ip, :fechaCreacion)
            """;

    private static final String ACTUALIZAR = """
            UPDATE solicitud_acceso SET
                nombres = :nombres, apellidos = :apellidos, cedula = :cedula, correo = :correo,
                id_unidad_academica = :idUnidad, justificacion = :justificacion, estado = :estado,
                token_hash = :tokenHash, token_expira = :tokenExpira, envios_verificacion = :envios,
                ultimo_envio = :ultimoEnvio, ip_origen = :ip, fecha_verificacion = :fechaVerificacion,
                id_revisor = :idRevisor, fecha_revision = :fechaRevision, motivo_rechazo = :motivo,
                bloqueada = :bloqueada
            WHERE id_solicitud = :id
            """;

    private static final String ELIMINAR_NO_VERIFICADAS = """
            DELETE FROM solicitud_acceso WHERE estado = 'PENDIENTE_VERIFICACION' AND token_expira < :ahora
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<SolicitudAcceso> buscarEnCurso(String correo, String cedula) {
        return primera(EN_CURSO, correoYCedula(correo, cedula));
    }

    @Override
    public Optional<SolicitudAcceso> buscarUltimaRechazada(String correo, String cedula) {
        return primera(ULTIMA_RECHAZADA, correoYCedula(correo, cedula));
    }

    @Override
    public Optional<SolicitudAcceso> buscarPorId(Long id) {
        return primera(POR_ID, new MapSqlParameterSource("id", id));
    }

    @Override
    public Optional<SolicitudAcceso> buscarPorTokenHash(String tokenHash) {
        return primera(POR_TOKEN, new MapSqlParameterSource("token", tokenHash));
    }

    /** Retorna null si otra solicitud en curso con el mismo correo, cédula o token ya existe. */
    @Override
    public Long crear(SolicitudAcceso solicitud) {
        KeyHolder llave = new GeneratedKeyHolder();
        try {
            jdbc.update(INSERTAR, parametros(solicitud), llave, new String[]{"id_solicitud"});
        } catch (DuplicateKeyException e) {
            return null;
        }
        Number id = llave.getKey();
        return id == null ? null : id.longValue();
    }

    @Override
    public void actualizar(SolicitudAcceso solicitud) {
        jdbc.update(ACTUALIZAR, parametros(solicitud)
                .addValue("id", solicitud.id(), Types.BIGINT)
                .addValue("fechaVerificacion", fecha(solicitud.fechaVerificacion()), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("idRevisor", solicitud.idRevisor(), Types.BIGINT)
                .addValue("fechaRevision", fecha(solicitud.fechaRevision()), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("motivo", solicitud.motivoRechazo(), Types.VARCHAR)
                .addValue("bloqueada", solicitud.bloqueada(), Types.BOOLEAN));
    }

    @Override
    public List<SolicitudAcceso> listarPorEstado(EstadoSolicitud estado) {
        return jdbc.query(POR_ESTADO, new MapSqlParameterSource("estado", estado.name()), fila());
    }

    @Override
    public long contarPorEstado(EstadoSolicitud estado) {
        Long total = jdbc.queryForObject(CONTAR_POR_ESTADO, new MapSqlParameterSource("estado", estado.name()), Long.class);
        return total == null ? 0 : total;
    }

    @Override
    public int eliminarNoVerificadasVencidas(Instant ahora) {
        return jdbc.update(ELIMINAR_NO_VERIFICADAS, new MapSqlParameterSource()
                .addValue("ahora", fecha(ahora), Types.TIMESTAMP_WITH_TIMEZONE));
    }

    private Optional<SolicitudAcceso> primera(String sql, MapSqlParameterSource parametros) {
        return jdbc.query(sql, parametros, fila()).stream().findFirst();
    }

    private static MapSqlParameterSource correoYCedula(String correo, String cedula) {
        return new MapSqlParameterSource()
                .addValue("correo", correo, Types.VARCHAR)
                .addValue("cedula", cedula, Types.VARCHAR);
    }

    private static MapSqlParameterSource parametros(SolicitudAcceso s) {
        return new MapSqlParameterSource()
                .addValue("nombres", s.nombres(), Types.VARCHAR)
                .addValue("apellidos", s.apellidos(), Types.VARCHAR)
                .addValue("cedula", s.cedula(), Types.VARCHAR)
                .addValue("correo", s.correo(), Types.VARCHAR)
                .addValue("idUnidad", s.idUnidadAcademica(), Types.BIGINT)
                .addValue("justificacion", s.justificacion(), Types.VARCHAR)
                .addValue("estado", s.estado().name(), Types.VARCHAR)
                .addValue("tokenHash", s.tokenHash(), Types.VARCHAR)
                .addValue("tokenExpira", fecha(s.tokenExpira()), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("envios", s.enviosVerificacion(), Types.INTEGER)
                .addValue("ultimoEnvio", fecha(s.ultimoEnvio()), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("ip", s.ipOrigen(), Types.VARCHAR)
                .addValue("fechaCreacion", fecha(s.fechaCreacion() != null ? s.fechaCreacion() : Instant.now()),
                        Types.TIMESTAMP_WITH_TIMEZONE);
    }

    private static RowMapper<SolicitudAcceso> fila() {
        return (rs, i) -> SolicitudAcceso.builder()
                .id(rs.getLong("id_solicitud"))
                .nombres(rs.getString("nombres"))
                .apellidos(rs.getString("apellidos"))
                .cedula(rs.getString("cedula"))
                .correo(rs.getString("correo"))
                .idUnidadAcademica(rs.getObject("id_unidad_academica") == null ? null : rs.getLong("id_unidad_academica"))
                .unidadAcademica(rs.getString("unidad_nombre"))
                .justificacion(rs.getString("justificacion"))
                .estado(EstadoSolicitud.valueOf(rs.getString("estado")))
                .tokenHash(rs.getString("token_hash"))
                .tokenExpira(instante(rs, "token_expira"))
                .enviosVerificacion(rs.getInt("envios_verificacion"))
                .ultimoEnvio(instante(rs, "ultimo_envio"))
                .ipOrigen(rs.getString("ip_origen"))
                .fechaCreacion(instante(rs, "fecha_creacion"))
                .fechaVerificacion(instante(rs, "fecha_verificacion"))
                .idRevisor(rs.getObject("id_revisor") == null ? null : rs.getLong("id_revisor"))
                .fechaRevision(instante(rs, "fecha_revision"))
                .motivoRechazo(rs.getString("motivo_rechazo"))
                .bloqueada(rs.getBoolean("bloqueada"))
                .build();
    }

    static OffsetDateTime fecha(Instant instante) {
        return instante == null ? null : instante.atOffset(ZoneOffset.UTC);
    }

    static Instant instante(ResultSet rs, String columna) throws SQLException {
        OffsetDateTime valor = rs.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : valor.toInstant();
    }
}
