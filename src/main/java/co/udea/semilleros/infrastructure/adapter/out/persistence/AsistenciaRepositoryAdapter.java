package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.asistencia.ConteoAsistencia;
import co.udea.semilleros.domain.model.asistencia.DatosSesion;
import co.udea.semilleros.domain.model.asistencia.EstadoAsistencia;
import co.udea.semilleros.domain.model.asistencia.IntegranteAsistencia;
import co.udea.semilleros.domain.model.asistencia.Sesion;
import co.udea.semilleros.domain.model.asistencia.SesionDetalle;
import co.udea.semilleros.domain.port.out.AsistenciaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Sesiones y asistencia con SQL estándar (PostgreSQL en ejecución, H2 en pruebas).
 */
@Component
@RequiredArgsConstructor
public class AsistenciaRepositoryAdapter implements AsistenciaRepositoryPort {

    // Conteo por estado sobre el alias "a" (asistencia_sesion)
    private static final String CONTEOS = """
            COALESCE(SUM(CASE WHEN a.estado = 'PRESENTE' THEN 1 ELSE 0 END), 0) AS presentes,
            COALESCE(SUM(CASE WHEN a.estado = 'AUSENTE'  THEN 1 ELSE 0 END), 0) AS ausentes,
            COALESCE(SUM(CASE WHEN a.estado = 'EXCUSADO' THEN 1 ELSE 0 END), 0) AS excusados
            """;

    private static final String SELECT_SESION = """
            SELECT ss.id_sesion, ss.id_semillero, ss.id_actividad, ac.nombre AS actividad, ss.titulo, ss.fecha,
            """ + CONTEOS + """
            FROM sesion_semillero ss
            LEFT JOIN actividad_cientifica ac ON ac.id_actividad = ss.id_actividad
            LEFT JOIN asistencia_sesion a ON a.id_sesion = ss.id_sesion
            """;

    private static final String AGRUPAR_SESION =
            " GROUP BY ss.id_sesion, ss.id_semillero, ss.id_actividad, ac.nombre, ss.titulo, ss.fecha";

    /** Sesiones (alias {@code ss}) dentro del rango; un extremo nulo no limita. */
    private static final String EN_RANGO = """
             AND (:desde IS NULL OR ss.fecha >= :desde)
             AND (:hasta IS NULL OR ss.fecha <= :hasta)
            """;

    private static final String SESION_POR_ID = SELECT_SESION + " WHERE ss.id_sesion = :id" + AGRUPAR_SESION;

    private static final String SESIONES_DEL_SEMILLERO = SELECT_SESION + " WHERE ss.id_semillero = :s" + EN_RANGO
            + AGRUPAR_SESION + " ORDER BY ss.fecha DESC, ss.id_sesion DESC";

    private static final String LISTA_DE_SESION = """
            SELECT si.id, si.nombres, si.apellidos, si.cedula, a.estado
            FROM asistencia_sesion a
            JOIN semillero_integrante si ON si.id = a.id_integrante
            WHERE a.id_sesion = :id
            ORDER BY LOWER(si.apellidos), LOWER(si.nombres)
            """;

    private static final String ASISTENCIA_POR_INTEGRANTE = """
            SELECT si.id, si.nombres, si.apellidos, si.cedula, si.activo,
            """ + CONTEOS + """
            FROM semillero_integrante si
            LEFT JOIN (SELECT a2.id_integrante, a2.estado
                       FROM asistencia_sesion a2
                       JOIN sesion_semillero ss ON ss.id_sesion = a2.id_sesion
                       WHERE ss.id_semillero = :s
            """ + EN_RANGO + """
            ) a ON a.id_integrante = si.id
            WHERE si.id_semillero = :s
            GROUP BY si.id, si.nombres, si.apellidos, si.cedula, si.activo
            HAVING si.activo = TRUE OR COUNT(a.estado) > 0
            ORDER BY LOWER(si.apellidos), LOWER(si.nombres)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<Long> idsIntegrantesActivos(Long idSemillero) {
        return jdbc.queryForList("SELECT id FROM semillero_integrante WHERE id_semillero = :s AND activo = TRUE ORDER BY id",
                new MapSqlParameterSource("s", idSemillero), Long.class);
    }

    @Override
    public List<Long> idsIntegrantesDeSesion(Long idSesion) {
        return jdbc.queryForList("SELECT id_integrante FROM asistencia_sesion WHERE id_sesion = :id",
                new MapSqlParameterSource("id", idSesion), Long.class);
    }

    @Override
    public boolean existeActividad(Long idActividad) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM actividad_cientifica WHERE id_actividad = :id",
                new MapSqlParameterSource("id", idActividad), Long.class);
        return total != null && total > 0;
    }

    @Override
    public Long crearSesion(Long idSemillero, DatosSesion datos) {
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO sesion_semillero (id_semillero, id_actividad, titulo, fecha)
                VALUES (:semillero, :actividad, :titulo, :fecha)
                """, parametrosSesion(datos).addValue("semillero", idSemillero), llave, new String[]{"id_sesion"});
        Long idSesion = Objects.requireNonNull(llave.getKey()).longValue();
        insertarAsistencias(idSesion, datos.asistencias());
        return idSesion;
    }

    @Override
    public void actualizarSesion(Long idSesion, DatosSesion datos) {
        jdbc.update("""
                UPDATE sesion_semillero
                SET id_actividad = :actividad, titulo = :titulo, fecha = :fecha, fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE id_sesion = :id
                """, parametrosSesion(datos).addValue("id", idSesion));
        jdbc.update("DELETE FROM asistencia_sesion WHERE id_sesion = :id", new MapSqlParameterSource("id", idSesion));
        insertarAsistencias(idSesion, datos.asistencias());
    }

    @Override
    public void eliminarSesion(Long idSesion) {
        MapSqlParameterSource parametros = new MapSqlParameterSource("id", idSesion);
        jdbc.update("DELETE FROM asistencia_sesion WHERE id_sesion = :id", parametros);
        jdbc.update("DELETE FROM sesion_semillero WHERE id_sesion = :id", parametros);
    }

    @Override
    public Optional<Long> semilleroDeSesion(Long idSesion) {
        return jdbc.queryForList("SELECT id_semillero FROM sesion_semillero WHERE id_sesion = :id",
                new MapSqlParameterSource("id", idSesion), Long.class).stream().findFirst();
    }

    @Override
    public Optional<SesionDetalle> obtenerSesion(Long idSesion) {
        MapSqlParameterSource parametros = new MapSqlParameterSource("id", idSesion);
        List<Sesion> sesion = jdbc.query(SESION_POR_ID, parametros, filaSesion());
        if (sesion.isEmpty()) {
            return Optional.empty();
        }
        List<SesionDetalle.Asistente> asistentes = jdbc.query(LISTA_DE_SESION, parametros, (rs, i) -> new SesionDetalle.Asistente(
                rs.getLong("id"), nombre(rs), rs.getString("cedula"), EstadoAsistencia.valueOf(rs.getString("estado"))));
        return Optional.of(new SesionDetalle(sesion.get(0), asistentes));
    }

    @Override
    public List<Sesion> listarSesiones(Long idSemillero, LocalDate desde, LocalDate hasta) {
        return jdbc.query(SESIONES_DEL_SEMILLERO, rango(idSemillero, desde, hasta), filaSesion());
    }

    @Override
    public List<IntegranteAsistencia> asistenciaPorIntegrante(Long idSemillero, LocalDate desde, LocalDate hasta) {
        return jdbc.query(ASISTENCIA_POR_INTEGRANTE, rango(idSemillero, desde, hasta), (rs, i) -> new IntegranteAsistencia(
                rs.getLong("id"), nombre(rs), rs.getString("cedula"), rs.getBoolean("activo"), conteo(rs)));
    }

    /** Semillero y extremos del rango con tipo SQL, para que PostgreSQL acepte los nulos. */
    private static MapSqlParameterSource rango(Long idSemillero, LocalDate desde, LocalDate hasta) {
        return new MapSqlParameterSource("s", idSemillero)
                .addValue("desde", desde, Types.DATE)
                .addValue("hasta", hasta, Types.DATE);
    }

    private void insertarAsistencias(Long idSesion, List<DatosSesion.Registro> asistencias) {
        SqlParameterSource[] filas = asistencias.stream()
                .map(registro -> new MapSqlParameterSource("sesion", idSesion)
                        .addValue("integrante", registro.idIntegrante())
                        .addValue("estado", registro.estado().name()))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate("""
                INSERT INTO asistencia_sesion (id_sesion, id_integrante, estado) VALUES (:sesion, :integrante, :estado)
                """, filas);
    }

    private static MapSqlParameterSource parametrosSesion(DatosSesion datos) {
        return new MapSqlParameterSource("titulo", datos.titulo().trim())
                .addValue("fecha", datos.fecha())
                .addValue("actividad", datos.idActividad(), Types.BIGINT);
    }

    private static RowMapper<Sesion> filaSesion() {
        return (rs, i) -> new Sesion(
                rs.getLong("id_sesion"),
                rs.getLong("id_semillero"),
                rs.getObject("id_actividad") == null ? null : rs.getLong("id_actividad"),
                rs.getString("actividad"),
                rs.getString("titulo"),
                rs.getObject("fecha", LocalDate.class),
                conteo(rs));
    }

    private static ConteoAsistencia conteo(ResultSet rs) throws SQLException {
        return new ConteoAsistencia(rs.getLong("presentes"), rs.getLong("ausentes"), rs.getLong("excusados"));
    }

    private static String nombre(ResultSet rs) throws SQLException {
        return (rs.getString("nombres") + " " + rs.getString("apellidos")).trim();
    }
}
