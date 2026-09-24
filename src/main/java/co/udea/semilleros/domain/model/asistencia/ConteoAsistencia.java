package co.udea.semilleros.domain.model.asistencia;

/**
 * Asistencias registradas y su porcentaje: presentes / (presentes + ausentes) × 100.
 * Las ausencias excusadas se descuentan del total esperado.
 */
public record ConteoAsistencia(long presentes, long ausentes, long excusados) {

    public static final ConteoAsistencia VACIO = new ConteoAsistencia(0, 0, 0);

    public long esperadas() {
        return presentes + ausentes;
    }

    /** Null cuando no hay asistencias esperadas (sin sesiones o todas excusadas). */
    public Double porcentaje() {
        return porcentaje(presentes, ausentes);
    }

    public static Double porcentaje(long presentes, long ausentes) {
        long esperadas = presentes + ausentes;
        return esperadas == 0 ? null : Math.round(presentes * 1000.0 / esperadas) / 10.0;
    }
}
