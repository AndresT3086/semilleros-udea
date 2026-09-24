package co.udea.semilleros.infrastructure.adapter.out.exportacion;

import co.udea.semilleros.domain.model.reporte.FormatoExportacion;
import co.udea.semilleros.domain.model.reporte.ReporteCompleto;
import co.udea.semilleros.domain.port.out.ExportadorReportePort;
import co.udea.semilleros.infrastructure.adapter.out.exportacion.SeccionesReporte.Seccion;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Genera los archivos de exportación de reportes (HU10) en CSV, Excel y PDF.
 */
@Component
public class ExportadorReporteAdapter implements ExportadorReportePort {

    static final String TITULO = "Reporte de semilleros de investigación";
    static final String INSTITUCION = "Universidad de Antioquia · Sistema de Gestión de Semilleros (SIGSI)";
    private static final Color VERDE_UDEA = new Color(0x02, 0x6e, 0x39);
    private static final Color VERDE_CLARO = new Color(0xe8, 0xf3, 0xed);
    private static final char SEPARADOR_CSV = ';';
    private static final byte[] BOM_UTF8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    @Override
    public byte[] exportar(ReporteCompleto reporte, FormatoExportacion formato) {
        return switch (formato) {
            case CSV -> csv(reporte);
            case XLSX -> xlsx(reporte);
            case PDF -> pdf(reporte);
        };
    }

    // ─── CSV: la tabla de rendimiento con los filtros aplicados ─────────────────

    byte[] csv(ReporteCompleto reporte) {
        Seccion tabla = SeccionesReporte.rendimiento(reporte.rendimiento());
        StringBuilder csv = new StringBuilder();
        agregarLineaCsv(csv, tabla.encabezados());
        tabla.filas().forEach(fila -> agregarLineaCsv(csv, fila));
        byte[] contenido = csv.toString().getBytes(StandardCharsets.UTF_8);
        // BOM para que Excel reconozca UTF-8 (tildes y ñ)
        byte[] conBom = new byte[BOM_UTF8.length + contenido.length];
        System.arraycopy(BOM_UTF8, 0, conBom, 0, BOM_UTF8.length);
        System.arraycopy(contenido, 0, conBom, BOM_UTF8.length, contenido.length);
        return conBom;
    }

    private static void agregarLineaCsv(StringBuilder csv, List<String> valores) {
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) {
                csv.append(SEPARADOR_CSV);
            }
            csv.append(celdaCsv(valores.get(i)));
        }
        csv.append("\r\n");
    }

    static String celdaCsv(String valor) {
        String texto = valor == null ? "" : valor;
        // Evita inyección de fórmulas al abrir el archivo en una hoja de cálculo
        if (!texto.isEmpty() && "=+-@".indexOf(texto.charAt(0)) >= 0 && !texto.matches("^[+-]?[\\d.,]+( %)?$")) {
            texto = "'" + texto;
        }
        if (texto.indexOf(SEPARADOR_CSV) >= 0 || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            texto = "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }

    // ─── Excel: una hoja de resumen y una hoja por sección ──────────────────────

    byte[] xlsx(ReporteCompleto reporte) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CellStyle titulo = estiloTitulo(libro);
            CellStyle encabezado = estiloEncabezado(libro);

            Sheet resumen = libro.createSheet("Resumen");
            int fila = 0;
            celda(resumen.createRow(fila++), 0, TITULO, titulo);
            celda(resumen.createRow(fila++), 0, INSTITUCION, null);
            fila++;
            celda(resumen.createRow(fila++), 0, "Filtros aplicados", encabezado);
            for (List<String> filtro : SeccionesReporte.filtros(reporte)) {
                Row row = resumen.createRow(fila++);
                celda(row, 0, filtro.get(0), null);
                celda(row, 1, filtro.get(1), null);
            }
            resumen.setColumnWidth(0, 30 * 256);
            resumen.setColumnWidth(1, 45 * 256);

            for (Seccion seccion : SeccionesReporte.secciones(reporte)) {
                Sheet hoja = libro.createSheet(WorkbookUtil.createSafeSheetName(recortar(seccion.titulo(), 31)));
                int r = 0;
                celda(hoja.createRow(r++), 0, seccion.titulo(), titulo);
                r++;
                Row cabecera = hoja.createRow(r++);
                for (int c = 0; c < seccion.encabezados().size(); c++) {
                    celda(cabecera, c, seccion.encabezados().get(c), encabezado);
                }
                for (List<String> valores : seccion.filas()) {
                    Row row = hoja.createRow(r++);
                    for (int c = 0; c < valores.size(); c++) {
                        celda(row, c, valores.get(c), null);
                    }
                }
                for (int c = 0; c < seccion.encabezados().size(); c++) {
                    hoja.setColumnWidth(c, (c == 0 ? 40 : 20) * 256);
                }
                hoja.createFreezePane(0, 3);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo Excel", e);
        }
    }

    private static void celda(Row fila, int columna, String valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor);
        if (estilo != null) {
            celda.setCellStyle(estilo);
        }
    }

    private static CellStyle estiloTitulo(XSSFWorkbook libro) {
        XSSFFont fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 14);
        fuente.setColor(new XSSFColor(VERDE_UDEA, null));
        XSSFCellStyle estilo = libro.createCellStyle();
        estilo.setFont(fuente);
        return estilo;
    }

    private static CellStyle estiloEncabezado(Workbook libro) {
        org.apache.poi.ss.usermodel.Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        XSSFCellStyle estilo = (XSSFCellStyle) libro.createCellStyle();
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(new XSSFColor(VERDE_UDEA, null));
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setBorderBottom(BorderStyle.THIN);
        return estilo;
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }

    // ─── PDF: informe con encabezado y pie de página institucional (RN37) ───────

    byte[] pdf(ReporteCompleto reporte) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 36, 36, 60, 50);
        try {
            PdfWriter escritor = PdfWriter.getInstance(documento, salida);
            escritor.setPageEvent(new EncabezadoYPie(reporte.generadoEn().format(SeccionesReporte.FECHA_HORA)));
            documento.addTitle(TITULO);
            documento.addAuthor("Universidad de Antioquia - SIGSI");
            documento.open();

            Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, VERDE_UDEA);
            Font fuenteSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, VERDE_UDEA);
            Font fuenteTexto = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font fuenteNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

            documento.add(new Paragraph(TITULO, fuenteTitulo));
            documento.add(new Paragraph(INSTITUCION, fuenteTexto));
            documento.add(Chunk.NEWLINE);

            PdfPTable filtros = new PdfPTable(new float[]{1, 3});
            filtros.setWidthPercentage(60);
            filtros.setHorizontalAlignment(Element.ALIGN_LEFT);
            for (List<String> filtro : SeccionesReporte.filtros(reporte)) {
                filtros.addCell(celdaPdf(filtro.get(0), fuenteNegrita, VERDE_CLARO));
                filtros.addCell(celdaPdf(filtro.get(1), fuenteTexto, null));
            }
            documento.add(filtros);

            for (Seccion seccion : SeccionesReporte.secciones(reporte)) {
                Paragraph titulo = new Paragraph(seccion.titulo(), fuenteSeccion);
                titulo.setSpacingBefore(14);
                titulo.setSpacingAfter(6);
                documento.add(titulo);
                if (seccion.filas().isEmpty()) {
                    documento.add(new Paragraph("Sin datos para los filtros aplicados.", fuenteTexto));
                    continue;
                }
                PdfPTable tabla = new PdfPTable(seccion.encabezados().size());
                tabla.setWidthPercentage(100);
                tabla.setHeaderRows(1);
                Font fuenteEncabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
                seccion.encabezados().forEach(e -> tabla.addCell(celdaPdf(e, fuenteEncabezado, VERDE_UDEA)));
                seccion.filas().forEach(fila -> fila.forEach(valor -> tabla.addCell(celdaPdf(valor, fuenteTexto, null))));
                documento.add(tabla);
            }
            documento.close();
            return salida.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el archivo PDF", e);
        }
    }

    private static PdfPCell celdaPdf(String texto, Font fuente, Color fondo) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "" : texto, fuente));
        celda.setPadding(4);
        celda.setBorderColor(new Color(0xcf, 0xdd, 0xd5));
        if (fondo != null) {
            celda.setBackgroundColor(fondo);
        }
        return celda;
    }

    /** Encabezado institucional y pie con fecha de generación y número de página en cada hoja. */
    static final class EncabezadoYPie extends PdfPageEventHelper {

        private final String generado;
        private final Font fuente = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);

        EncabezadoYPie(String generado) {
            this.generado = generado;
        }

        @Override
        public void onEndPage(PdfWriter escritor, Document documento) {
            Rectangle pagina = documento.getPageSize();
            float izquierda = documento.left();
            float derecha = documento.right();
            var lienzo = escritor.getDirectContent();

            lienzo.setColorStroke(VERDE_UDEA);
            lienzo.setLineWidth(1.5f);
            lienzo.moveTo(izquierda, pagina.getTop() - 40);
            lienzo.lineTo(derecha, pagina.getTop() - 40);
            lienzo.stroke();
            ColumnText.showTextAligned(lienzo, Element.ALIGN_LEFT,
                    new Phrase("UdeA · SIGSI — Reportes y estadísticas", fuente), izquierda, pagina.getTop() - 34, 0);

            lienzo.setLineWidth(0.5f);
            lienzo.moveTo(izquierda, 38);
            lienzo.lineTo(derecha, 38);
            lienzo.stroke();
            ColumnText.showTextAligned(lienzo, Element.ALIGN_LEFT,
                    new Phrase(INSTITUCION + " · Generado el " + generado, fuente), izquierda, 26, 0);
            ColumnText.showTextAligned(lienzo, Element.ALIGN_RIGHT,
                    new Phrase("Página " + escritor.getPageNumber(), fuente), derecha, 26, 0);
        }
    }
}
