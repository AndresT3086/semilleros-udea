package co.udea.semilleros.domain.model.reporte;

/**
 * Archivo exportado listo para descargar.
 */
public record ReporteArchivo(String nombre, String contentType, byte[] contenido) {
}
