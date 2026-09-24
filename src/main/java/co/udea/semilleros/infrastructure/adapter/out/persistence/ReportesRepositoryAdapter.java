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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Consultas agregadas de reportes con SQL estándar (PostgreSQL en ejecución, H2 en pruebas).
 * Las condiciones se agregan solo para los filtros presentes, así PostgreSQL nunca
 * recibe parámetros nulos sin tipo.
 */
@Component
@RequiredArgsConstructor
public class ReportesRepositoryAdapter implements ReportesRepositoryPort {

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

    static final String SIN_CLASIFICAR = "SIN_CLASIFICAR";
    private static final String NO_INFORMADO = "NO_INFORMADO";
    private static final Map<String, String> NOMBRES_SEXO = new LinkedHashMap<>();

    static {
        NOMBRES_SEXO.put("FEMENINO", "Femenino");
        NOMBRES_SEXO.put("MASCULINO", "Masculino");
        NOMBRES_SEXO.put("OTRO", "Otro");
        NOMBRES_SEXO.put(NO_INFORMADO, "No informado");
    }

    private static final Map<OrdenRendimiento, String> COLUMNAS_ORDEN = new EnumMap<>(Map.of(
            OrdenRendimiento.NOMBRE, "LOWER(r.nombre)",
            OrdenRendimiento.UNIDAD, "LOWER(r.unidad)",
            OrdenRendimiento.TIPO, "LOWER(r.unidad)",
            OrdenRendimiento.CAMPUS, "LOWER(r.campus)",
            OrdenRendimiento.PARTICIPANTES, "r.participantes",
            OrdenRendimiento.ACTIVIDADES, "r.actividades",
            OrdenRendimiento.SESIONES, "r.sesiones",
            OrdenRendimiento.ASISTENCIA, "porcentaje_asistencia",
            OrdenRendimiento.ESTADO, "r.estado"
    ));

    private final NamedParameterJdbcTemplate jdbc;

    /** Condiciones WHERE sobre los alias {@code s} (semillero) y {@code ua} (unidad académica). */
    record Condiciones(String sql, MapSqlParameterSource parametros) {

        /** Integrantes vinculados hasta la fecha de corte del filtro (alias {@code si}). */
        String integrantesHastaCorte() {
            return parametros.hasValue("fechaCorte")
                    ? " AND (si.fecha_ingreso IS NULL OR si.fecha_ingreso <= :fechaCorte)"
                    : "";
        }

        /** Sesiones (alias {@code ss}) cuya fecha cae dentro del período del filtro. */
        String sesionesEnPeriodo() {
            return (parametros.hasValue("inicioPeriodo") ? " AND ss.fecha >= :inicioPeriodo" : "")
                    + (parametros.hasValue("fechaCorte") ? " AND ss.fecha <= :fechaCorte" : "");
        }
    }

    static Condiciones condiciones(ReporteFiltro filtro, boolean incluirInactivos) {
        StringBuilder sql = new StringBuilder(incluirInactivos
                ? " WHERE s.estado IN ('ACTIVO', 'INACTIVO')"
                : " WHERE s.estado = 'ACTIVO'");
        MapSqlParameterSource parametros = new MapSqlParameterSource();
        if (filtro.anioCorte() != null) {
            sql.append(" AND (s.anio_creacion IS NULL OR s.anio_creacion <= :anioCorte)");
            parametros.addValue("anioCorte", filtro.anioCorte());
        }
        if (filtro.fechaCorte() != null) {
            parametros.addValue("fechaCorte", filtro.fechaCorte());
        }
        if (filtro.inicioPeriodo() != null) {
            parametros.addValue("inicioPeriodo", filtro.inicioPeriodo());
        }
        agregarIgualdad(sql, parametros, "s.id_unidad_academica", "idUnidad", filtro.idUnidad());
        agregarIgualdad(sql, parametros, "s.id_campus", "idCampus", filtro.idCampus());
        agregarIgualdad(sql, parametros, "s.id_semillero", "idSemillero", filtro.idSemillero());
        agregarIgualdad(sql, parametros, "s.id_coordinador", "idCoordinador", filtro.idCoordinador());
        if (filtro.tipoUnidad() != null) {
            // Prefijo sin la parte que puede llevar tilde ("corporac" coincide con "Corporación")
            String prefijo = filtro.tipoUnidad().getPrefijo();
            sql.append(" AND LOWER(ua.nombre) LIKE :tipoUnidad");
            parametros.addValue("tipoUnidad", prefijo.substring(0, Math.min(8, prefijo.length())) + "%");
        }
        return new Condiciones(sql.toString(), parametros);
    }

    private static void agregarIgualdad(StringBuilder sql, MapSqlParameterSource parametros,
                                        String columna, String nombre, Long valor) {
        if (valor != null) {
            sql.append(" AND ").append(columna).append(" = :").append(nombre);
            parametros.addValue(nombre, valor);
        }
    }

    @Override
    public long contarSemillerosActivos(ReporteFiltro filtro) {
        Condiciones c = condiciones(filtro, false);
        return contar("SELECT COUNT(*)" + DESDE_SEMILLERO + c.sql(), c.parametros());
    }

    @Override
    public long contarIntegrantesRegistrados(ReporteFiltro filtro) {
        Condiciones c = condiciones(filtro, false);
        return contar("SELECT COUNT(DISTINCT si.cedula)" + DESDE_INTEGRANTE + c.sql() + c.integrantesHastaCorte(),
                c.parametros());
    }

    @Override
    public long contarIntegrantesActivos(ReporteFiltro filtro) {
        Condiciones c = condiciones(filtro, false);
        return contar("SELECT COUNT(DISTINCT si.cedula)" + DESDE_INTEGRANTE + c.sql() + c.integrantesHastaCorte()
                + " AND si.activo = TRUE", c.parametros());
    }

    @Override
    public long contarActividadesRealizadas(ReporteFiltro filtro) {
        Condiciones c = condiciones(filtro, false);
        return contar("SELECT COUNT(*)" + DESDE_SESION + c.sql() + c.sesionesEnPeriodo(), c.parametros());
    }

    @Override
    public List<ReporteUnidad> distribucionPorUnidad(ReporteFiltro filtro) {
        Condiciones c = condiciones(filtro, false);
        String sql = """
                SELECT ua.id_unidad AS id, ua.nombre AS nombre,
                       COUNT(DISTINCT s.id_semillero) AS semilleros,
                       COUNT(DISTINCT CASE WHEN si.activo = TRUE %s THEN si.cedula END) AS estudiantes
                 FROM semillero s
                 JOIN unidad_academica ua ON ua.id_unidad = s.id_unidad_academica
                 LEFT JOIN semillero_integrante si ON si.id_semillero = s.id_semillero
                """.formatted(c.integrantesHastaCorte())
                + c.sql()
                + " GROUP BY ua.id_unidad, ua.nombre ORDER BY semilleros DESC, ua.nombre";
        return jdbc.query(sql, c.parametros(), (rs, i) -> new ReporteUnidad(
                rs.getLong("id"),
                rs.getString("nombre"),
                TipoUnidad.desdeNombre(rs.getString("nombre")),
                rs.getLong("semilleros"),
                rs.getLong("estudiantes")));
    }

    /** Todas las sedes, incluso sin semilleros, para comparar concentraciones (HU4). */
    @Override
    public List<ReporteConteo> distribucionPorCampus(ReporteFiltro filtro) {
        Condiciones c = condiciones(filtro, false);
        String sql = """
                SELECT ca.id_campus AS id, ca.nombre AS nombre, COALESCE(x.cantidad, 0) AS cantidad
                 FROM campus ca
                 LEFT JOIN (SELECT s.id_campus AS id_campus, COUNT(*) AS cantidad
                """ + DESDE_SEMILLERO + c.sql() + """
                 GROUP BY s.id_campus) x ON x.id_campus = ca.id_campus
                 ORDER BY cantidad DESC, ca.nombre
                """;
        return jdbc.query(sql, c.parametros(), conteo());
    }

    @Override
    public List<ReporteConteo> integrantesPorSexo(ReporteFiltro filtro) {
        Condiciones c = condiciones(filtro, false);
        String sql = "SELECT COALESCE(si.sexo, '" + NO_INFORMADO + "') AS sexo, COUNT(DISTINCT si.cedula) AS cantidad"
                + DESDE_INTEGRANTE + c.sql() + c.integrantesHastaCorte() + " AND si.activo = TRUE"
                + " GROUP BY COALESCE(si.sexo, '" + NO_INFORMADO + "')";
        Map<String, Long> cantidades = new LinkedHashMap<>();
        jdbc.query(sql, c.parametros(), rs -> {
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
        Condiciones c = condiciones(filtro, false);
        String sql = """
                SELECT ri.codigo AS id, ri.nombre AS nombre, COALESCE(x.cantidad, 0) AS cantidad
                 FROM rol_integrante ri
                 LEFT JOIN (SELECT si.tipo_vinculacion AS codigo, COUNT(*) AS cantidad
                """ + DESDE_INTEGRANTE + c.sql() + c.integrantesHastaCorte() + """
                 AND si.activo = TRUE
                 GROUP BY si.tipo_vinculacion) x ON x.codigo = ri.codigo
                 ORDER BY ri.orden
                """;
        return jdbc.query(sql, c.parametros(), conteo());
    }

    @Override
    public Map<Integer, Long> semillerosCreadosPorAnio(ReporteFiltro filtro) {
        Condiciones c = condiciones(filtro, false);
        String sql = "SELECT s.anio_creacion AS anio, COUNT(*) AS cantidad" + DESDE_SEMILLERO + c.sql()
                + " AND s.anio_creacion IS NOT NULL GROUP BY s.anio_creacion ORDER BY s.anio_creacion";
        Map<Integer, Long> resultado = new LinkedHashMap<>();
        jdbc.query(sql, c.parametros(), rs -> {
            resultado.put(rs.getInt("anio"), rs.getLong("cantidad"));
        });
        return resultado;
    }

    /** Actividades registradas por tipo del catálogo del formulario de caracterización (RN28). */
    @Override
    public List<ReporteConteo> actividadesPorTipo(ReporteFiltro filtro) {
        Condiciones c = condiciones(filtro, false);
        String sql = """
                SELECT ac.id_actividad AS id, ac.nombre AS nombre, COALESCE(x.cantidad, 0) AS cantidad
                 FROM actividad_cientifica ac
                 LEFT JOIN (SELECT ss.id_actividad AS id_actividad, COUNT(*) AS cantidad
                """ + DESDE_SESION + c.sql() + c.sesionesEnPeriodo() + """
                 GROUP BY ss.id_actividad) x ON x.id_actividad = ac.id_actividad
                 ORDER BY ac.id_actividad
                """;
        List<ReporteConteo> resultado = new ArrayList<>(jdbc.query(sql, c.parametros(), conteo()));
        long sinClasificar = contar("SELECT COUNT(*)" + DESDE_SESION + c.sql() + c.sesionesEnPeriodo()
                + " AND ss.id_actividad IS NULL", c.parametros());
        if (sinClasificar > 0) {
            resultado.add(new ReporteConteo(SIN_CLASIFICAR, "Sin clasificar", sinClasificar));
        }
        return resultado;
    }

    @Override
    public ReporteAsistencia asistencia(ReporteFiltro filtro) {
        Condiciones c = condiciones(filtro, false);
        String sql = """
                SELECT COUNT(DISTINCT ss.id_sesion) AS sesiones,
                       COALESCE(SUM(CASE WHEN a.estado = 'PRESENTE' THEN 1 ELSE 0 END), 0) AS presentes,
                       COALESCE(SUM(CASE WHEN a.estado = 'AUSENTE'  THEN 1 ELSE 0 END), 0) AS ausentes,
                       COALESCE(SUM(CASE WHEN a.estado = 'EXCUSADO' THEN 1 ELSE 0 END), 0) AS excusados
                """ + DESDE_SESION + " LEFT JOIN asistencia_sesion a ON a.id_sesion = ss.id_sesion"
                + c.sql() + c.sesionesEnPeriodo();
        return jdbc.queryForObject(sql, c.parametros(), (rs, i) -> new ReporteAsistencia(rs.getLong("sesiones"),
                new ConteoAsistencia(rs.getLong("presentes"), rs.getLong("ausentes"), rs.getLong("excusados"))));
    }

    @Override
    public PageResult<ReporteRendimiento> rendimiento(ReporteFiltro filtro, int pagina, int tamano,
                                                      OrdenRendimiento orden, boolean ascendente) {
        Condiciones c = condiciones(filtro, true);
        long total = contar("SELECT COUNT(*)" + DESDE_SEMILLERO + c.sql(), c.parametros());
        c.parametros().addValue("limite", tamano).addValue("desplazamiento", (long) pagina * tamano);
        List<ReporteRendimiento> filas = jdbc.query(
                consultaRendimiento(c, orden, ascendente) + " LIMIT :limite OFFSET :desplazamiento",
                c.parametros(), filaRendimiento());
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
        Condiciones c = condiciones(filtro, true);
        return jdbc.query(consultaRendimiento(c, orden, ascendente), c.parametros(), filaRendimiento());
    }

    private static String consultaRendimiento(Condiciones c, OrdenRendimiento orden, boolean ascendente) {
        String columna = COLUMNAS_ORDEN.get(Optional.ofNullable(orden).orElse(OrdenRendimiento.NOMBRE));
        String asistencia = "(SELECT COALESCE(SUM(CASE WHEN a.estado = '%s' THEN 1 ELSE 0 END), 0)"
                + " FROM asistencia_sesion a JOIN sesion_semillero ss ON ss.id_sesion = a.id_sesion"
                + " WHERE ss.id_semillero = s.id_semillero" + c.sesionesEnPeriodo() + ")";
        // La consulta interna calcula las métricas; la externa permite ordenar por el porcentaje
        return """
                SELECT r.*, CASE WHEN r.presentes + r.ausentes = 0 THEN NULL
                                 ELSE r.presentes * 100.0 / (r.presentes + r.ausentes) END AS porcentaje_asistencia
                FROM (
                SELECT s.id_semillero AS id, s.nombre AS nombre, s.codigo AS codigo,
                       ua.nombre AS unidad, c.nombre AS campus, s.estado AS estado,
                       (SELECT COUNT(DISTINCT si.cedula) FROM semillero_integrante si
                         WHERE si.id_semillero = s.id_semillero AND si.activo = TRUE %s) AS participantes,
                       (SELECT COUNT(*) FROM semillero_actividad sa
                         WHERE sa.id_semillero = s.id_semillero AND sa.realiza = TRUE) AS actividades,
                       (SELECT COUNT(*) FROM sesion_semillero ss
                         WHERE ss.id_semillero = s.id_semillero %s) AS sesiones,
                       %s AS presentes,
                       %s AS ausentes
                """.formatted(c.integrantesHastaCorte(), c.sesionesEnPeriodo(),
                        asistencia.formatted("PRESENTE"), asistencia.formatted("AUSENTE"))
                + DESDE_SEMILLERO
                + " LEFT JOIN campus c ON c.id_campus = s.id_campus"
                + c.sql()
                + ") r ORDER BY " + columna + (ascendente ? " ASC" : " DESC") + " NULLS LAST, r.id";
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
        Condiciones c = condiciones(filtro, false);
        String sql = "SELECT s.id_semillero AS id, s.nombre AS nombre" + DESDE_SEMILLERO + c.sql()
                + " AND s.nombre IS NOT NULL ORDER BY LOWER(s.nombre)";
        return jdbc.query(sql, c.parametros(), (rs, i) -> new ReporteOpcion(rs.getLong("id"), rs.getString("nombre")));
    }

    @Override
    public String huellaDatos() {
        return jdbc.queryForObject("""
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
                """, new MapSqlParameterSource(), String.class);
    }

    private long contar(String sql, MapSqlParameterSource parametros) {
        Long total = jdbc.queryForObject(sql, parametros, Long.class);
        return total == null ? 0 : total;
    }

    private static RowMapper<ReporteConteo> conteo() {
        return (rs, i) -> new ReporteConteo(rs.getString("id"), rs.getString("nombre"), rs.getLong("cantidad"));
    }
}
