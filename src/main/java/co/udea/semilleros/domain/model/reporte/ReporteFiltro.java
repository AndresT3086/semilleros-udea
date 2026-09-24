package co.udea.semilleros.domain.model.reporte;

import co.udea.semilleros.domain.exception.FiltroReporteInvalidoException;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filtros de los reportes (HU2, HU14) junto con el alcance de quien consulta (HU12).
 *
 * @param periodo       período académico tal como se pidió ("2025", "2025-1") o null
 * @param anioCorte     último año de creación de semilleros incluido, o null para el estado actual
 * @param fechaCorte    última fecha de ingreso de integrantes incluida, o null para el estado actual
 * @param tipoUnidad    tipo de unidad académica o null para todas
 * @param idCoordinador semilleros de este coordinador (alcance COORDINADOR) o null
 */
public record ReporteFiltro(
        String periodo,
        Integer anioCorte,
        LocalDate fechaCorte,
        TipoUnidad tipoUnidad,
        Long idUnidad,
        Long idCampus,
        Long idSemillero,
        AlcanceReporte alcance,
        Long idCoordinador
) {

    private static final Pattern PERIODO = Pattern.compile("^(\\d{4})(?:-([12]))?$");

    public static ReporteFiltro de(String periodo, String tipoUnidad, Long idUnidad,
                                   Long idCampus, Long idSemillero) {
        String periodoLimpio = periodo == null || periodo.isBlank() ? null : periodo.trim();
        Integer anio = null;
        LocalDate corte = null;
        if (periodoLimpio != null) {
            Matcher matcher = PERIODO.matcher(periodoLimpio);
            if (!matcher.matches()) {
                throw new FiltroReporteInvalidoException(
                        "El período debe tener el formato AAAA o AAAA-S (ej: 2025 o 2025-1).");
            }
            anio = Integer.parseInt(matcher.group(1));
            corte = "1".equals(matcher.group(2))
                    ? LocalDate.of(anio, 6, 30)
                    : LocalDate.of(anio, 12, 31);
        }
        return new ReporteFiltro(periodoLimpio, anio, corte, parsearTipo(tipoUnidad),
                idUnidad, idCampus, idSemillero, AlcanceReporte.ADMIN, null);
    }

    private static TipoUnidad parsearTipo(String tipoUnidad) {
        if (tipoUnidad == null || tipoUnidad.isBlank()) {
            return null;
        }
        TipoUnidad tipo;
        try {
            tipo = TipoUnidad.valueOf(tipoUnidad.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            tipo = TipoUnidad.OTRA;
        }
        if (tipo == TipoUnidad.OTRA) {
            throw new FiltroReporteInvalidoException(
                    "Tipo de unidad no válido: " + tipoUnidad
                            + ". Valores permitidos: FACULTAD, ESCUELA, INSTITUTO, CORPORACION, SECCIONAL.");
        }
        return tipo;
    }

    public ReporteFiltro paraCoordinador(Long idCoordinadorAutenticado) {
        return new ReporteFiltro(periodo, anioCorte, fechaCorte, tipoUnidad, idUnidad, idCampus,
                idSemillero, AlcanceReporte.COORDINADOR, idCoordinadorAutenticado);
    }

    public ReporteFiltro paraPublico() {
        return new ReporteFiltro(periodo, anioCorte, fechaCorte, tipoUnidad, idUnidad, idCampus,
                idSemillero, AlcanceReporte.PUBLICO, null);
    }

    public ReporteFiltro sinSemillero() {
        return new ReporteFiltro(periodo, anioCorte, fechaCorte, tipoUnidad, idUnidad, idCampus,
                null, alcance, idCoordinador);
    }

    public ReporteFiltro sinPeriodo() {
        return new ReporteFiltro(null, null, null, tipoUnidad, idUnidad, idCampus,
                idSemillero, alcance, idCoordinador);
    }

    /**
     * Período comparable anterior para calcular tendencias (RN4): año anterior,
     * semestre anterior o, sin período, el mes anterior a la fecha indicada.
     */
    public ReporteFiltro periodoAnterior(LocalDate hoy) {
        String anterior;
        int anio;
        LocalDate corte;
        if (periodo == null) {
            corte = hoy.minusMonths(1);
            anio = corte.getYear();
            anterior = null;
        } else if (periodo.endsWith("-2")) {
            anio = anioCorte;
            corte = LocalDate.of(anio, 6, 30);
            anterior = anio + "-1";
        } else if (periodo.endsWith("-1")) {
            anio = anioCorte - 1;
            corte = LocalDate.of(anio, 12, 31);
            anterior = anio + "-2";
        } else {
            anio = anioCorte - 1;
            corte = LocalDate.of(anio, 12, 31);
            anterior = String.valueOf(anio);
        }
        return new ReporteFiltro(anterior, anio, corte, tipoUnidad, idUnidad, idCampus,
                idSemillero, alcance, idCoordinador);
    }
}
