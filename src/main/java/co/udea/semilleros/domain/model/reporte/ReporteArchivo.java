package co.udea.semilleros.domain.model.reporte;

import java.util.Arrays;
import java.util.Objects;

/**
 * Archivo exportado listo para descargar. Compara el contenido del arreglo, no su referencia.
 */
public record ReporteArchivo(String nombre, String contentType, byte[] contenido) {

    @Override
    public boolean equals(Object otro) {
        return otro instanceof ReporteArchivo archivo
                && Objects.equals(nombre, archivo.nombre)
                && Objects.equals(contentType, archivo.contentType)
                && Arrays.equals(contenido, archivo.contenido);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(nombre, contentType) + Arrays.hashCode(contenido);
    }

    @Override
    public String toString() {
        return "ReporteArchivo[nombre=" + nombre + ", contentType=" + contentType
                + ", bytes=" + (contenido == null ? 0 : contenido.length) + "]";
    }
}
