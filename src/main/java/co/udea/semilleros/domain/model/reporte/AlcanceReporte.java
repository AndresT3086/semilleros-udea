package co.udea.semilleros.domain.model.reporte;

/**
 * Nivel de detalle permitido según el rol de quien consulta (HU12).
 */
public enum AlcanceReporte {
    /** Administrador: KPIs globales, todos los gráficos y la tabla completa. */
    ADMIN,
    /** Coordinador: solo los semilleros que coordina. */
    COORDINADOR,
    /** Semillerista o visitante: agregados anónimos, sin detalle por semillero. */
    PUBLICO
}
