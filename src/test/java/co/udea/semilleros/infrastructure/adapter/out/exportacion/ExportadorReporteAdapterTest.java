package co.udea.semilleros.infrastructure.adapter.out.exportacion;

import co.udea.semilleros.domain.model.asistencia.ConteoAsistencia;
import co.udea.semilleros.domain.model.reporte.AlcanceReporte;
import co.udea.semilleros.domain.model.reporte.FormatoExportacion;
import co.udea.semilleros.domain.model.reporte.ReporteAsistencia;
import co.udea.semilleros.domain.model.reporte.ReporteCompleto;
import co.udea.semilleros.domain.model.reporte.ReporteConteo;
import co.udea.semilleros.domain.model.reporte.ReporteDashboard;
import co.udea.semilleros.domain.model.reporte.ReporteEvolucion;
import co.udea.semilleros.domain.model.reporte.ReporteFiltro;
import co.udea.semilleros.domain.model.reporte.ReporteKpis;
import co.udea.semilleros.domain.model.reporte.ReporteRendimiento;
import co.udea.semilleros.domain.model.reporte.ReporteUnidad;
import co.udea.semilleros.domain.model.reporte.TipoUnidad;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExportadorReporteAdapter - Exportación a CSV, Excel y PDF")
class ExportadorReporteAdapterTest {

    private final ExportadorReporteAdapter exportador = new ExportadorReporteAdapter();

    private static ReporteCompleto reporte(List<ReporteRendimiento> filas) {
        ReporteKpis kpis = new ReporteKpis(4, 10, 8, 12, 80.0,
                new ReporteKpis.Tendencias(33.3, null, -5.0, 50.0, 2.5), "2024",
                LocalDateTime.of(2026, 9, 24, 10, 30), AlcanceReporte.ADMIN);
        ReporteDashboard dashboard = new ReporteDashboard(kpis,
                List.of(new ReporteUnidad(1L, "Facultad de Ingeniería", TipoUnidad.FACULTAD, 3, 9)),
                List.of(new ReporteConteo("1", "Medellín", 3), new ReporteConteo("2", "Apartadó", 1)),
                List.of(new ReporteUnidad(1L, "Facultad de Ingeniería", TipoUnidad.FACULTAD, 3, 9)),
                List.of(new ReporteConteo("FEMENINO", "Femenino", 5), new ReporteConteo("MASCULINO", "Masculino", 3)),
                List.of(new ReporteConteo("TUTOR", "Tutor", 0)),
                List.of(new ReporteEvolucion(2025, 3, 3, false), new ReporteEvolucion(2026, 4, 1, true)),
                List.of(),
                new ReporteAsistencia(12, new ConteoAsistencia(90, 10, 5)));
        return new ReporteCompleto(ReporteFiltro.de("2025", "FACULTAD", 1L, 2L, null), dashboard, filas,
                LocalDateTime.of(2026, 9, 24, 10, 30));
    }

    private static List<ReporteRendimiento> filas() {
        return List.of(
                new ReporteRendimiento(1L, "Semillero IA; \"Aplicada\"", "SEM-1", "Facultad de Ingeniería",
                        TipoUnidad.FACULTAD, "Medellín", 1200, 4, 0, null, "ACTIVO"),
                new ReporteRendimiento(2L, null, "SEM-2", null, null, null, 0, 0, 8, 87.5, "INACTIVO"));
    }

    @Test
    @DisplayName("CSV: tabla de rendimiento en UTF-8 con BOM, separador ';' y valores escapados")
    void csv_tablaDeRendimiento() {
        byte[] archivo = exportador.exportar(reporte(filas()), FormatoExportacion.CSV);

        assertThat(archivo).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        String[] lineas = new String(archivo, 3, archivo.length - 3, StandardCharsets.UTF_8).split("\r\n");
        assertThat(lineas).hasSize(3);
        assertThat(lineas[0]).isEqualTo("Semillero;Código;Unidad académica;Tipo;Campus;Participantes;"
                + "Actividades registradas;Tipos de actividad;% Asistencia;Estado");
        assertThat(lineas[1]).isEqualTo("\"Semillero IA; \"\"Aplicada\"\"\";SEM-1;Facultad de Ingeniería;Facultad;"
                + "Medellín;1.200;0;4;No disponible;ACTIVO");
        assertThat(lineas[2]).isEqualTo("(sin nombre);SEM-2;;;;0;8;0;87,5 %;INACTIVO");
    }

    @Test
    @DisplayName("CSV: neutraliza fórmulas pero conserva números con signo")
    void csv_evitaInyeccionDeFormulas() {
        assertThat(ExportadorReporteAdapter.celdaCsv("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(ExportadorReporteAdapter.celdaCsv("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(ExportadorReporteAdapter.celdaCsv("-5,0 %")).isEqualTo("-5,0 %");
        assertThat(ExportadorReporteAdapter.celdaCsv("+12")).isEqualTo("+12");
        assertThat(ExportadorReporteAdapter.celdaCsv("línea\nnueva")).isEqualTo("\"línea\nnueva\"");
        assertThat(ExportadorReporteAdapter.celdaCsv(null)).isEmpty();
    }

    @Test
    @DisplayName("Excel: hoja de resumen con filtros y una hoja por sección")
    void xlsx_hojas() throws Exception {
        byte[] archivo = exportador.exportar(reporte(filas()), FormatoExportacion.XLSX);

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            List<String> hojas = new ArrayList<>();
            libro.sheetIterator().forEachRemaining(hoja -> hojas.add(hoja.getSheetName()));
            assertThat(hojas).containsExactly("Resumen", "Indicadores clave", "Rendimiento por semillero",
                    "Semilleros por unidad académica", "Top facultades con más semiller",
                    "Semilleros por campus o seccion", "Integrantes por sexo", "Integrantes según rol desempeña",
                    "Evolución de semilleros activos", "Actividades registradas por tip");

            Sheet resumen = libro.getSheet("Resumen");
            assertThat(resumen.getRow(0).getCell(0).getStringCellValue()).isEqualTo(ExportadorReporteAdapter.TITULO);
            assertThat(resumen.getRow(4).getCell(1).getStringCellValue()).isEqualTo("2025");
            assertThat(resumen.getRow(5).getCell(1).getStringCellValue()).isEqualTo("Facultad");
            assertThat(resumen.getRow(6).getCell(1).getStringCellValue()).isEqualTo("Facultad de Ingeniería");
            assertThat(resumen.getRow(7).getCell(1).getStringCellValue()).isEqualTo("Apartadó");
            assertThat(resumen.getRow(8).getCell(1).getStringCellValue()).isEqualTo("Todos");

            Sheet kpis = libro.getSheet("Indicadores clave");
            assertThat(kpis.getRow(2).getCell(2).getStringCellValue()).isEqualTo("Variación vs 2024");
            assertThat(kpis.getRow(3).getCell(2).getStringCellValue()).isEqualTo("+33,3 %");
            assertThat(kpis.getRow(4).getCell(2).getStringCellValue()).isEqualTo("No disponible");
            assertThat(kpis.getRow(5).getCell(2).getStringCellValue()).isEqualTo("-5,0 %");
            assertThat(kpis.getRow(7).getCell(1).getStringCellValue()).isEqualTo("80,0 %");
            assertThat(kpis.getRow(7).getCell(2).getStringCellValue()).isEqualTo("+2,5 pp");
            assertThat(kpis.getRow(6).getCell(2).getStringCellValue()).isEqualTo("+50,0 %");
            assertThat(kpis.getRow(8).getCell(1).getStringCellValue()).isEqualTo("90,0 %");

            Sheet sexo = libro.getSheet("Integrantes por sexo");
            assertThat(sexo.getRow(3).getCell(2).getStringCellValue()).isEqualTo("62,5 %");
            Sheet roles = libro.getSheet("Integrantes según rol desempeña");
            assertThat(roles.getRow(3).getCell(2).getStringCellValue()).isEqualTo("No disponible");
            Sheet evolucion = libro.getSheet("Evolución de semilleros activos");
            assertThat(evolucion.getRow(4).getCell(3).getStringCellValue()).isEqualTo("Proyección");
        }
    }

    @Test
    @DisplayName("PDF: informe con título, filtros, secciones y pie de página numerado")
    void pdf_informe() throws Exception {
        byte[] archivo = exportador.exportar(reporte(filas()), FormatoExportacion.PDF);

        assertThat(new String(archivo, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        PdfReader lector = new PdfReader(archivo);
        try {
            String texto = new PdfTextExtractor(lector).getTextFromPage(1);
            assertThat(texto).contains(ExportadorReporteAdapter.TITULO)
                    .contains("Período", "2025")
                    .contains("Indicadores clave")
                    .contains("Página 1")
                    .contains("Generado el 24/09/2026 10:30");
        } finally {
            lector.close();
        }
    }

    @Test
    @DisplayName("PDF: secciones sin datos muestran un mensaje en lugar de una tabla vacía")
    void pdf_seccionesVacias() throws Exception {
        byte[] archivo = exportador.exportar(reporte(List.of()), FormatoExportacion.PDF);

        PdfReader lector = new PdfReader(archivo);
        try {
            StringBuilder texto = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(lector);
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                texto.append(extractor.getTextFromPage(pagina));
            }
            assertThat(texto.toString()).contains("Sin datos para los filtros aplicados.");
        } finally {
            lector.close();
        }
    }
}
