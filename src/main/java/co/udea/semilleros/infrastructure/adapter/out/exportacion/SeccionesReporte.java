package co.udea.semilleros.infrastructure.adapter.out.exportacion;

import co.udea.semilleros.domain.model.reporte.ReporteAsistencia;
import co.udea.semilleros.domain.model.reporte.ReporteCompleto;
import co.udea.semilleros.domain.model.reporte.ReporteConteo;
import co.udea.semilleros.domain.model.reporte.ReporteDashboard;
import co.udea.semilleros.domain.model.reporte.ReporteFiltro;
import co.udea.semilleros.domain.model.reporte.ReporteKpis;
import co.udea.semilleros.domain.model.reporte.ReporteRendimiento;
import co.udea.semilleros.domain.model.reporte.ReporteUnidad;
import co.udea.semilleros.domain.model.reporte.TipoUnidad;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Convierte un reporte en tablas (título, encabezados y filas de texto) comunes
 * a todos los formatos de exportación.
 */
final class SeccionesReporte {

    static final Locale ES_CO = Locale.forLanguageTag("es-CO");
    static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final String NO_DISPONIBLE = "No disponible";

    record Seccion(String titulo, List<String> encabezados, List<List<String>> filas) {
    }

    private SeccionesReporte() {
    }

    /** Filtros aplicados en texto legible, resolviendo nombres con los datos del propio reporte. */
    static List<List<String>> filtros(ReporteCompleto reporte) {
        ReporteFiltro filtro = reporte.filtro();
        ReporteDashboard dashboard = reporte.dashboard();
        List<List<String>> filas = new ArrayList<>();
        filas.add(List.of("Período", filtro.periodo() == null ? "Estado actual" : filtro.periodo()));
        filas.add(List.of("Tipo de unidad", filtro.tipoUnidad() == null ? "Todos" : nombreTipo(filtro.tipoUnidad())));
        filas.add(List.of("Unidad académica", nombrePorId(filtro.idUnidad(), dashboard.porUnidad(),
                ReporteUnidad::id, ReporteUnidad::nombre, "Todas")));
        filas.add(List.of("Campus o seccional", nombrePorId(filtro.idCampus(), dashboard.porCampus(),
                conteo -> Long.valueOf(conteo.id()), ReporteConteo::nombre, "Todos")));
        filas.add(List.of("Semillero", nombrePorId(filtro.idSemillero(), reporte.rendimiento(),
                ReporteRendimiento::id, ReporteRendimiento::nombre, "Todos")));
        filas.add(List.of("Generado", reporte.generadoEn().format(FECHA_HORA)));
        return filas;
    }

    static List<Seccion> secciones(ReporteCompleto reporte) {
        ReporteDashboard d = reporte.dashboard();
        List<Seccion> secciones = new ArrayList<>();
        secciones.add(kpis(d.kpis(), d.asistencia()));
        secciones.add(rendimiento(reporte.rendimiento()));
        secciones.add(new Seccion("Semilleros por unidad académica",
                List.of("Unidad académica", "Tipo", "Semilleros", "Estudiantes"),
                d.porUnidad().stream().map(u -> fila(u.nombre(), nombreTipo(u.tipo()), numero(u.semilleros()),
                        numero(u.estudiantes()))).toList()));
        secciones.add(new Seccion("Top facultades con más semilleros",
                List.of("Posición", "Facultad", "Semilleros"),
                enumerar(d.topFacultades(), (posicion, u) -> fila(posicion, u.nombre(), numero(u.semilleros())))));
        secciones.add(conPorcentaje("Semilleros por campus o seccional", "Campus", d.porCampus()));
        secciones.add(conPorcentaje("Integrantes por sexo", "Sexo", d.porSexo()));
        secciones.add(conPorcentaje("Integrantes según rol desempeñado", "Rol", d.porRol()));
        secciones.add(new Seccion("Evolución de semilleros activos",
                List.of("Año", "Semilleros activos", "Creados en el año", "Tipo de dato"),
                d.evolucion().stream().map(e -> fila(String.valueOf(e.anio()), numero(e.semillerosActivos()),
                        numero(e.nuevos()), e.proyectado() ? "Proyección" : "Registrado")).toList()));
        secciones.add(conPorcentaje("Actividades por tipo (semilleros que las realizan)", "Actividad",
                d.actividadesPorTipo()));
        return secciones;
    }

    static Seccion rendimiento(List<ReporteRendimiento> filas) {
        return new Seccion("Rendimiento por semillero",
                List.of("Semillero", "Código", "Unidad académica", "Tipo", "Campus", "Participantes",
                        "Actividades registradas", "Tipos de actividad", "% Asistencia", "Estado"),
                filas.stream().map(r -> fila(
                        Objects.requireNonNullElse(r.nombre(), "(sin nombre)"),
                        r.codigo(),
                        Objects.requireNonNullElse(r.unidadAcademica(), ""),
                        nombreTipo(r.tipoUnidad()),
                        Objects.requireNonNullElse(r.campus(), ""),
                        numero(r.participantes()),
                        numero(r.sesiones()),
                        numero(r.actividadesRealizadas()),
                        r.porcentajeAsistencia() == null ? NO_DISPONIBLE : porcentaje(r.porcentajeAsistencia()),
                        r.estado())).toList());
    }

    private static Seccion kpis(ReporteKpis k, ReporteAsistencia asistencia) {
        String comparado = "Variación vs " + k.periodoComparado();
        return new Seccion("Indicadores clave", List.of("Indicador", "Valor", comparado), List.of(
                fila("Semilleros activos", numero(k.semillerosActivos()), variacion(k.tendencias().semillerosActivos())),
                fila("Usuarios registrados", numero(k.usuariosRegistrados()), variacion(k.tendencias().usuariosRegistrados())),
                fila("Miembros activos", numero(k.miembrosActivos()), variacion(k.tendencias().miembrosActivos())),
                fila("Actividades realizadas", numero(k.actividadesRealizadas()), NO_DISPONIBLE),
                fila("Tasa de participación",
                        k.tasaParticipacion() == null ? NO_DISPONIBLE : porcentaje(k.tasaParticipacion()),
                        k.tendencias().tasaParticipacion() == null ? NO_DISPONIBLE
                                : signo(k.tendencias().tasaParticipacion()) + " pp"),
                fila("Actividades registradas con asistencia", numero(asistencia.sesiones()), NO_DISPONIBLE),
                fila("% Asistencia (excusas descontadas)", asistencia.asistencia().porcentaje() == null ? NO_DISPONIBLE
                        : porcentaje(asistencia.asistencia().porcentaje()), NO_DISPONIBLE)));
    }

    private static Seccion conPorcentaje(String titulo, String categoria, List<ReporteConteo> conteos) {
        long total = conteos.stream().mapToLong(ReporteConteo::cantidad).sum();
        return new Seccion(titulo, List.of(categoria, "Cantidad", "Porcentaje"),
                conteos.stream().map(c -> fila(c.nombre(), numero(c.cantidad()),
                        total == 0 ? NO_DISPONIBLE : porcentaje(c.cantidad() * 100.0 / total))).toList());
    }

    private static <T> List<List<String>> enumerar(List<T> elementos,
                                                   java.util.function.BiFunction<String, T, List<String>> fila) {
        List<List<String>> filas = new ArrayList<>();
        for (int i = 0; i < elementos.size(); i++) {
            filas.add(fila.apply(String.valueOf(i + 1), elementos.get(i)));
        }
        return filas;
    }

    private static <T> String nombrePorId(Long id, List<T> elementos, Function<T, Long> obtenerId,
                                          Function<T, String> obtenerNombre, String todos) {
        if (id == null) {
            return todos;
        }
        return elementos.stream()
                .filter(elemento -> id.equals(obtenerId.apply(elemento)))
                .map(obtenerNombre)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("#" + id);
    }

    static String nombreTipo(TipoUnidad tipo) {
        if (tipo == null) {
            return "";
        }
        return switch (tipo) {
            case FACULTAD -> "Facultad";
            case ESCUELA -> "Escuela";
            case INSTITUTO -> "Instituto";
            case CORPORACION -> "Corporación";
            case SECCIONAL -> "Seccional";
            case OTRA -> "Otra";
        };
    }

    private static List<String> fila(String... valores) {
        return Arrays.asList(valores);
    }

    static String numero(long valor) {
        return NumberFormat.getIntegerInstance(ES_CO).format(valor);
    }

    static String porcentaje(double valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(ES_CO);
        formato.setMaximumFractionDigits(1);
        formato.setMinimumFractionDigits(1);
        return formato.format(valor) + " %";
    }

    private static String variacion(Double valor) {
        return valor == null ? NO_DISPONIBLE : signo(valor) + " %";
    }

    private static String signo(double valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(ES_CO);
        formato.setMaximumFractionDigits(1);
        formato.setMinimumFractionDigits(1);
        return (valor > 0 ? "+" : "") + formato.format(valor);
    }
}
