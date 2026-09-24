package co.udea.semilleros.domain.model.reporte;

import co.udea.semilleros.domain.exception.FiltroReporteInvalidoException;

import java.util.Locale;

/**
 * Formatos de exportación de reportes (HU10).
 */
public enum FormatoExportacion {
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PDF("pdf", "application/pdf"),
    CSV("csv", "text/csv;charset=UTF-8");

    private final String extension;
    private final String contentType;

    FormatoExportacion(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String getExtension() {
        return extension;
    }

    public String getContentType() {
        return contentType;
    }

    public static FormatoExportacion de(String valor) {
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new FiltroReporteInvalidoException("Formato de exportación no soportado: " + valor
                    + ". Use xlsx, pdf o csv.");
        }
    }
}
