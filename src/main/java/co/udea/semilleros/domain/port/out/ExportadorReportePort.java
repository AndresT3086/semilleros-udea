package co.udea.semilleros.domain.port.out;

import co.udea.semilleros.domain.model.reporte.FormatoExportacion;
import co.udea.semilleros.domain.model.reporte.ReporteCompleto;

/**
 * Genera el archivo de un reporte en el formato solicitado (HU10).
 */
public interface ExportadorReportePort {

    byte[] exportar(ReporteCompleto reporte, FormatoExportacion formato);
}
