package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.asistencia.ConteoAsistencia;
import co.udea.semilleros.domain.model.reporte.OrdenRendimiento;
import co.udea.semilleros.domain.model.reporte.ReporteAsistencia;
import co.udea.semilleros.domain.model.reporte.ReporteConteo;
import co.udea.semilleros.domain.model.reporte.ReporteFiltro;
import co.udea.semilleros.domain.model.reporte.ReporteOpcion;
import co.udea.semilleros.domain.model.reporte.ReporteRendimiento;
import co.udea.semilleros.domain.model.reporte.ReporteUnidad;
import co.udea.semilleros.domain.model.reporte.TipoUnidad;
import co.udea.semilleros.domain.port.out.ReportesRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Consultas agregadas de reportes con SQL estándar (PostgreSQL en ejecución, H2 en pruebas).
 * Todas las consultas son constantes: los filtros opcionales se expresan como
 * {@code (:parametro IS NULL OR condición)} y los parámetros se envían con su tipo SQL,
 * así PostgreSQL puede evaluar los nulos y ningún valor se concatena en el SQL.
 */
@Component
@RequiredArgsConstructor
public class ReportesRepositoryAdapter implements ReportesRepositoryPort {

    static final String SIN_CLASIFICAR = "SIN_CLASIFICAR";
    private static final String NO_INFORMADO = "NO_INFORMADO";

    private static final String DESDE_SEMILLERO = """
             FROM semillero s
             LEFT JOIN unidad_academica ua ON ua.id_unidad = s.id_unidad_academica
            """;

    private static final String DESDE_INTEGRANTE = """
             FROM semillero_integrante si
             JOIN semillero s ON s.id_semillero = si.id_semillero
             LEFT JOIN unidad_academica ua ON ua.id_unidad = s.id_unidad_academica
            """;

    private static final String DESDE_SESION = """
             FROM sesion_semillero ss
             JOIN semillero s ON s.id_semillero = ss.id_semillero
             LEFT JOIN unidad_academica ua ON ua.id_unidad = s.id_unidad_academica
            """;

    /** Filtros y alcance sobre los alias {@code s} (semillero) y {@code ua} (unidad académica). */
    private static final String FILTRO = """
             WHERE (s.estado = 'ACTIVO' OR (:incluirInactivos = TRUE AND s.estado = 'INACTIVO'))
               AND (:anioCorte IS NULL OR s.anio_creacion IS NULL OR s.anio_creacion <= :anioCorte)
               AND (:idUnidad IS NULL OR s.id_unidad_academica = :idUnidad)
               AND (:idCampus IS NULL OR s.id_campus = :idCampus)
               AND (:idSemillero IS NULL OR s.id_semillero = :idSemillero)
               AND (:idCoordinador IS NULL OR s.id_coordinador = :idCoordinador)
               AND (:tipoUnidad IS NULL OR LOWER(ua.nombre) LIKE :tipoUnidad)
            """;

    /** Integrantes (alias {@code si}) vinculados hasta la fecha de corte del período. */
    private static final String INTEGRANTE_HASTA_CORTE = """
               AND (:fechaCorte IS NULL OR si.fecha_ingreso IS NULL OR si.fecha_ingreso <= :fechaCorte)
            """;

    /** Sesiones (alias {@code ss}) con fecha dentro del período. */
    private static final String SESION_EN_PERIODO = """
               AND (:inicioPeriodo IS NULL OR ss.fecha >= :inicioPeriodo)
               AND (:fechaCorte IS NULL OR ss.fecha <= :fechaCorte)
            """;

    private static final String CONTAR_SEMILLEROS = "SELECT COUNT(*)" + DESDE_SEMILLERO + FILTRO;

    private static final String CONTAR_REGISTRADOS =
            "SELECT COUNT(DISTINCT si.cedula)" + DESDE_INTEGRANTE + FILTRO + INTEGRANTE_HASTA_CORTE;

    private static final String CONTAR_ACTIVOS = CONTAR_REGISTRADOS + " AND si.activo = TRUE";

    private static final String CONTAR_SESIONES = "SELECT COUNT(*)" + DESDE_SESION + FILTRO + SESION_EN_PERIODO;

    private static final String CONTAR_SESIONES_SIN_TIPO = CONTAR_SESIONES + " AND ss.id_actividad IS NULL";

    private static final String POR_UNIDAD = """
            SELECT ua.id_unidad AS id, ua.nombre AS nombre,
                   COUNT(DISTINCT s.id_semillero) AS semilleros,
                   COUNT(DISTINCT CASE WHEN si.activo = TRUE
                        AND (:fechaCorte IS NULL OR si.fecha_ingreso IS NULL OR si.fecha_ingreso <= :fechaCorte)
                        THEN si.cedula END) AS estudiantes
             FROM semillero s
             JOIN unidad_academica ua ON ua.id_unidad = s.id_unidad_academica
             LEFT JOIN semillero_integrante si ON si.id_semillero = s.id_semillero
            """ + FILTRO + " GROUP BY ua.id_unidad, ua.nombre ORDER BY semilleros DESC, ua.nombre";

    private static final String POR_CAMPUS = """
            SELECT ca.id_campus AS id, ca.nombre AS nombre, COALESCE(x.cantidad, 0) AS cantidad
             FROM campus ca
             LEFT JOIN (SELECT s.id_campus AS id_campus, COUNT(*) AS cantidad
            """ + DESDE_SEMILLERO + FILTRO + """
             GROUP BY s.id_campus) x ON x.id_campus = ca.id_campus
             ORDER BY cantidad DESC, ca.nombre
            """;

    private static final String POR_SEXO = "SELECT COALESCE(si.sexo, '" + NO_INFORMADO + "') AS sexo,"
            + " COUNT(DISTINCT si.cedula) AS cantidad" + DESDE_INTEGRANTE + FILTRO + INTEGRANTE_HASTA_CORTE
            + " AND si.activo = TRUE GROUP BY COALESCE(si.sexo, '" + NO_INFORMADO + "')";

    private static final String POR_ROL = """
            SELECT ri.codigo AS id, ri.nombre AS nombre, COALESCE(x.cantidad, 0) AS cantidad
             FROM rol_integrante ri
             LEFT JOIN (SELECT si.tipo_vinculacion AS codigo, COUNT(*) AS cantidad
            """ + DESDE_INTEGRANTE + FILTRO + INTEGRANTE_HASTA_CORTE + """
               AND si.activo = TRUE
             GROUP BY si.tipo_vinculacion) x ON x.codigo = ri.codigo
             ORDER BY ri.orden
            """;

    private static final String CREADOS_POR_ANIO = "SELECT s.anio_creacion AS anio, COUNT(*) AS cantidad"
            + DESDE_SEMILLERO + FILTRO
            + " AND s.anio_creacion IS NOT NULL GROUP BY s.anio_creacion ORDER BY s.anio_creacion";

    private static final String ACTIVIDADES_POR_TIPO = """
            SELECT ac.id_actividad AS id, ac.nombre AS nombre, COALESCE(x.cantidad, 0) AS cantidad
             FROM actividad_cientifica ac
             LEFT JOIN (SELECT ss.id_actividad AS id_actividad, COUNT(*) AS cantidad
            """ + DESDE_SESION + FILTRO + SESION_EN_PERIODO + """
             GROUP BY ss.id_actividad) x ON x.id_actividad = ac.id_actividad
             ORDER BY ac.id_actividad
            """;

    private static final String ASISTENCIA = """
            SELECT COUNT(DISTINCT ss.id_sesion) AS sesiones,
                   COALESCE(SUM(CASE WHEN a.estado = 'PRESENTE' THEN 1 ELSE 0 END), 0) AS presentes,
                   COALESCE(SUM(CASE WHEN a.estado = 'AUSENTE'  THEN 1 ELSE 0 END), 0) AS ausentes,
                   COALESCE(SUM(CASE WHEN a.estado = 'EXCUSADO' THEN 1 ELSE 0 END), 0) AS excusados
            """ + DESDE_SESION + " LEFT JOIN asistencia_sesion a ON a.id_sesion = ss.id_sesion"
            + FILTRO + SESION_EN_PERIODO;

    private static final String ASISTENCIA_SEMILLERO = """
             FROM asistencia_sesion a
             JOIN sesion_semillero ss ON ss.id_sesion = a.id_sesion
             WHERE ss.id_semillero = s.id_semillero
            """ + SESION_EN_PERIODO;

    /**
     * La consulta interna calcula las métricas por semillero, la intermedia el porcentaje y
     * el ORDER BY elige la columna con CASE (texto y números por separado para no mezclar tipos).
     */
    private static final String RENDIMIENTO = """
            SELECT t.* FROM (
              SELECT r.*, CASE WHEN r.presentes + r.ausentes = 0 THEN NULL
                               ELSE r.presentes * 100.0 / (r.presentes + r.ausentes) END AS porcentaje_asistencia
              FROM (
                SELECT s.id_semillero AS id, s.nombre AS nombre, s.codigo AS codigo,
                       ua.nombre AS unidad, c.nombre AS campus, s.estado AS estado,
                       (SELECT COUNT(DISTINCT si.cedula) FROM semillero_integrante si
                         WHERE si.id_semillero = s.id_semillero AND si.activo = TRUE
            """ + INTEGRANTE_HASTA_CORTE + """
                       ) AS participantes,
                       (SELECT COUNT(*) FROM semillero_actividad sa
                         WHERE sa.id_semillero = s.id_semillero AND sa.realiza = TRUE) AS actividades,
                       (SELECT COUNT(*) FROM sesion_semillero ss
                         WHERE ss.id_semillero = s.id_semillero
            """ + SESION_EN_PERIODO + """
                       ) AS sesiones,
                       (SELECT COALESCE(SUM(CASE WHEN a.estado = 'PRESENTE' THEN 1 ELSE 0 END), 0)
            """ + ASISTENCIA_SEMILLERO + """
                       ) AS presentes,
                       (SELECT COALESCE(SUM(CASE WHEN a.estado = 'AUSENTE' THEN 1 ELSE 0 END), 0)
            """ + ASISTENCIA_SEMILLERO + """
                       ) AS ausentes
            """ + DESDE_SEMILLERO + " LEFT JOIN campus c ON c.id_campus = s.id_campus" + FILTRO + """
              ) r
            ) t
            ORDER BY
              CASE WHEN :ascendente = TRUE THEN
                CASE :orden WHEN 'NOMBRE' THEN LOWER(t.nombre) WHEN 'UNIDAD' THEN LOWER(t.unidad)
                            WHEN 'TIPO' THEN LOWER(t.unidad) WHEN 'CAMPUS' THEN LOWER(t.campus)
                            WHEN 'ESTADO' THEN t.estado END END ASC NULLS LAST,
              CASE WHEN :ascendente = FALSE THEN
                CASE :orden WHEN 'NOMBRE' THEN LOWER(t.nombre) WHEN 'UNIDAD' THEN LOWER(t.unidad)
                            WHEN 'TIPO' THEN LOWER(t.unidad) WHEN 'CAMPUS' THEN LOWER(t.campus)
                            WHEN 'ESTADO' THEN t.estado END END DESC NULLS LAST,
              CASE WHEN :ascendente = TRUE THEN
                CASE :orden WHEN 'PARTICIPANTES' THEN t.participantes WHEN 'ACTIVIDADES' THEN t.actividades
                            WHEN 'SESIONES' THEN t.sesiones WHEN 'ASISTENCIA' THEN t.porcentaje_asistencia END
                END ASC NULLS LAST,
              CASE WHEN :ascendente = FALSE THEN
                CASE :orden WHEN 'PARTICIPANTES' THEN t.participantes WHEN 'ACTIVIDADES' THEN t.actividades
                            WHEN 'SESIONES' THEN t.sesiones WHEN 'ASISTENCIA' THEN t.porcentaje_asistencia END
                END DESC NULLS LAST,
              t.id
            """;

    private static final String RENDIMIENTO_PAGINADO = RENDIMIENTO + " LIMIT :limite OFFSET :desplazamiento";

    private static final String SEMILLEROS_ACTIVOS = "SELECT s.id_semillero AS id, s.nombre AS nombre"
            + DESDE_SEMILLERO + FILTRO + " AND s.nombre IS NOT NULL ORDER BY LOWER(s.nombre)";

    private static final String HUELLA = """
            SELECT CONCAT(
              (SELECT COUNT(*) FROM semillero), '|',
              (SELECT COALESCE(CAST(MAX(fecha_actualizacion) AS VARCHAR(40)), '-') FROM semillero), '|',
              (SELECT COUNT(*) FROM semillero_integrante), '|',
              (SELECT COUNT(*) FROM semillero_integrante WHERE activo = TRUE), '|',
              (SELECT COUNT(*) FROM semillero_actividad WHERE realiza = TRUE), '|',
              (SELECT COUNT(*) FROM inscripcion), '|',
              (SELECT COUNT(*) FROM sesion_semillero), '|',
              (SELECT COUNT(*) FROM asistencia_sesion), '|',
              (SELECT COALESCE(CAST(MAX(COALESCE(fecha_actualizacion, fecha_creacion)) AS VARCHAR(40)), '-')
                 FROM sesion_semillero), '|',
              (SELECT COALESCE(CAST(MAX(fecha_actualizacion) AS VARCHAR(40)), '-') FROM inscripcion))
            """;

    private static final Map<String, String> NOMBRES_SEXO = new LinkedHashMap<>();

    static {
        NOMBRES_SEXO.put("FEMENINO", "Femenino");
        NOMBRES_SEXO.put("MASCULINO", "Masculino");
        NOMBRES_SEXO.put("OTRO", "Otro");
        NOMBRES_SEXO.put(NO_INFORMADO, "No informado");
    }

    private final NamedParameterJdbcTemplate jdbc;

    /** Todos los parámetros de {@link #FILTRO} y de los rangos de fecha, con su tipo SQL. */
    static MapSqlParameterSource parametros(ReporteFiltro filtro, boolean incluirInactivos) {
        String tipoUnidad = null;
        if (filtro.tipoUnidad() != null) {
            // Prefijo sin la parte que puede llevar tilde ("corporac" coincide con "Corporación")
            String prefijo = filtro.tipoUnidad().getPrefijo();
            tipoUnidad = prefijo.substring(0, Math.min(8, prefijo.length())) + "%";
        }
        return new MapSqlParameterSource()
                .addValue("incluirInactivos", incluirInactivos, Types.BOOLEAN)
                .addValue("anioCorte", filtro.anioCorte(), Types.INTEGER)
                .addValue("fechaCorte", filtro.fechaCorte(), Types.DATE)
                .addValue("inicioPeriodo", filtro.inicioPeriodo(), Types.DATE)
                .addValue("idUnidad", filtro.idUnidad(), Types.BIGINT)
                .addValue("idCampus", filtro.idCampus(), Types.BIGINT)
                .addValue("idSemillero", filtro.idSemillero(), Types.BIGINT)
                .addValue("idCoordinador", filtro.idCoordinador(), Types.BIGINT)
                .addValue("tipoUnidad", tipoUnidad, Types.VARCHAR);
    }

    @Override
    public long contarSemillerosActivos(ReporteFiltro filtro) {
        return contar(CONTAR_SEMILLEROS, parametros(filtro, false));
    }

    @Override
    public long contarIntegrantesRegistrados(ReporteFiltro filtro) {
        return contar(CONTAR_REGISTRADOS, parametros(filtro, false));
    }

    @Override
    public long contarIntegrantesActivos(ReporteFiltro filtro) {
        return contar(CONTAR_ACTIVOS, parametros(filtro, false));
    }

    @Override
    public long contarActividadesRealizadas(ReporteFiltro filtro) {
        return contar(CONTAR_SESIONES, parametros(filtro, false));
    }

    @Override
    public List<ReporteUnidad> distribucionPorUnidad(ReporteFiltro filtro) {
        return jdbc.query(POR_UNIDAD, parametros(filtro, false), (rs, i) -> new ReporteUnidad(
                rs.getLong("id"),
                rs.getString("nombre"),
                TipoUnidad.desdeNombre(rs.getString("nombre")),
                rs.getLong("semilleros"),
                rs.getLong("estudiantes")));
    }

    /** Todas las sedes, incluso sin semilleros, para comparar concentraciones (HU4). */
    @Override
    public List<ReporteConteo> distribucionPorCampus(ReporteFiltro filtro) {
        return jdbc.query(POR_CAMPUS, parametros(filtro, false), conteo());
    }

    @Override
    public List<ReporteConteo> integrantesPorSexo(ReporteFiltro filtro) {
        Map<String, Long> cantidades = new LinkedHashMap<>();
        jdbc.query(POR_SEXO, parametros(filtro, false), rs -> {
            cantidades.put(rs.getString("sexo"), rs.getLong("cantidad"));
        });
        List<ReporteConteo> resultado = new ArrayList<>();
        NOMBRES_SEXO.forEach((codigo, nombre) -> {
            long cantidad = cantidades.getOrDefault(codigo, 0L);
            // Femenino y Masculino siempre se muestran; el resto solo si hay datos
            if (cantidad > 0 || "FEMENINO".equals(codigo) || "MASCULINO".equals(codigo)) {
                resultado.add(new ReporteConteo(codigo, nombre, cantidad));
            }
        });
        return resultado;
    }

    /** Asignaciones de rol por integrante activo según el catálogo rol_integrante (RN17). */
    @Override
    public List<ReporteConteo> integrantesPorRol(ReporteFiltro filtro) {
        return jdbc.query(POR_ROL, parametros(filtro, false), conteo());
    }

    @Override
    public Map<Integer, Long> semillerosCreadosPorAnio(ReporteFiltro filtro) {
        Map<Integer, Long> resultado = new LinkedHashMap<>();
        jdbc.query(CREADOS_POR_ANIO, parametros(filtro, false), rs -> {
            resultado.put(rs.getInt("anio"), rs.getLong("cantidad"));
        });
        return resultado;
    }

    /** Actividades registradas por tipo del catálogo del formulario de caracterización (RN28). */
    @Override
    public List<ReporteConteo> actividadesPorTipo(ReporteFiltro filtro) {
        MapSqlParameterSource parametros = parametros(filtro, false);
        List<ReporteConteo> resultado = new ArrayList<>(jdbc.query(ACTIVIDADES_POR_TIPO, parametros, conteo()));
        long sinClasificar = contar(CONTAR_SESIONES_SIN_TIPO, parametros);
        if (sinClasificar > 0) {
            resultado.add(new ReporteConteo(SIN_CLASIFICAR, "Sin clasificar", sinClasificar));
        }
        return resultado;
    }

    @Override
    public ReporteAsistencia asistencia(ReporteFiltro filtro) {
        return jdbc.queryForObject(ASISTENCIA, parametros(filtro, false), (rs, i) -> new ReporteAsistencia(
                rs.getLong("sesiones"),
                new ConteoAsistencia(rs.getLong("presentes"), rs.getLong("ausentes"), rs.getLong("excusados"))));
    }

    @Override
    public PageResult<ReporteRendimiento> rendimiento(ReporteFiltro filtro, int pagina, int tamano,
                                                      OrdenRendimiento orden, boolean ascendente) {
        long total = contar(CONTAR_SEMILLEROS, parametros(filtro, true));
        MapSqlParameterSource parametros = parametrosRendimiento(filtro, orden, ascendente)
                .addValue("limite", tamano, Types.INTEGER)
                .addValue("desplazamiento", (long) pagina * tamano, Types.BIGINT);
        List<ReporteRendimiento> filas = jdbc.query(RENDIMIENTO_PAGINADO, parametros, filaRendimiento());
        int totalPaginas = (int) Math.ceil((double) total / tamano);
        return PageResult.<ReporteRendimiento>builder()
                .contenido(filas)
                .paginaActual(pagina)
                .tamano(tamano)
                .totalElementos(total)
                .totalPaginas(totalPaginas)
                .esPrimeraPagina(pagina == 0)
                .esUltimaPagina(pagina >= totalPaginas - 1)
                .build();
    }

    @Override
    public List<ReporteRendimiento> rendimientoCompleto(ReporteFiltro filtro, OrdenRendimiento orden,
                                                        boolean ascendente) {
        return jdbc.query(RENDIMIENTO, parametrosRendimiento(filtro, orden, ascendente), filaRendimiento());
    }

    private static MapSqlParameterSource parametrosRendimiento(ReporteFiltro filtro, OrdenRendimiento orden,
                                                               boolean ascendente) {
        return parametros(filtro, true)
                .addValue("orden", Objects.requireNonNullElse(orden, OrdenRendimiento.NOMBRE).name(), Types.VARCHAR)
                .addValue("ascendente", ascendente, Types.BOOLEAN);
    }

    private static RowMapper<ReporteRendimiento> filaRendimiento() {
        return (rs, i) -> new ReporteRendimiento(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getString("codigo"),
                rs.getString("unidad"),
                TipoUnidad.desdeNombre(rs.getString("unidad")),
                rs.getString("campus"),
                rs.getLong("participantes"),
                rs.getLong("actividades"),
                rs.getLong("sesiones"),
                ConteoAsistencia.porcentaje(rs.getLong("presentes"), rs.getLong("ausentes")),
                rs.getString("estado"));
    }

    @Override
    public List<ReporteOpcion> semillerosActivos(ReporteFiltro filtro) {
        return jdbc.query(SEMILLEROS_ACTIVOS, parametros(filtro, false),
                (rs, i) -> new ReporteOpcion(rs.getLong("id"), rs.getString("nombre")));
    }

    @Override
    public String huellaDatos() {
        return jdbc.queryForObject(HUELLA, new MapSqlParameterSource(), String.class);
    }

    private long contar(String sql, MapSqlParameterSource parametros) {
        Long total = jdbc.queryForObject(sql, parametros, Long.class);
        return total == null ? 0 : total;
    }

    private static RowMapper<ReporteConteo> conteo() {
        return (rs, i) -> new ReporteConteo(rs.getString("id"), rs.getString("nombre"), rs.getLong("cantidad"));
    }
}
