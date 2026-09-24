package co.udea.semilleros.domain.model.reporte;

import java.time.LocalDateTime;

/**
 * Indicadores clave (HU1). {@code actividadesRealizadas} cuenta las actividades registradas
 * por los coordinadores con fecha dentro del período. Las variaciones son porcentuales frente al período
 * comparable anterior y valen null cuando no hay base de comparación.
 */
public record ReporteKpis(
        long semillerosActivos,
        long usuariosRegistrados,
        long miembrosActivos,
        long actividadesRealizadas,
        Double tasaParticipacion,
        Tendencias tendencias,
        String periodoComparado,
        LocalDateTime fechaCalculo,
        AlcanceReporte alcance
) {
    public record Tendencias(
            Double semillerosActivos,
            Double usuariosRegistrados,
            Double miembrosActivos,
            Double actividadesRealizadas,
            Double tasaParticipacion
    ) {
    }
}
