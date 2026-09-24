package co.udea.semilleros.domain.model.reporte;

import co.udea.semilleros.domain.exception.FiltroReporteInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReporteFiltro y enums de reportes - Pruebas unitarias")
class ReporteFiltroTest {

    @ParameterizedTest
    @CsvSource({
            "2025,   2025, 2025-12-31",
            "2025-1, 2025, 2025-06-30",
            "2025-2, 2025, 2025-12-31"
    })
    @DisplayName("de: interpreta años completos y semestres (RN7)")
    void de_interpretaPeriodos(String periodo, int anio, LocalDate corte) {
        ReporteFiltro filtro = ReporteFiltro.de(periodo, null, null, null, null);

        assertThat(filtro.anioCorte()).isEqualTo(anio);
        assertThat(filtro.fechaCorte()).isEqualTo(corte);
        assertThat(filtro.alcance()).isEqualTo(AlcanceReporte.ADMIN);
    }

    @Test
    @DisplayName("de: sin período consulta el estado actual")
    void de_sinPeriodo() {
        ReporteFiltro filtro = ReporteFiltro.de("  ", "", 1L, 2L, 3L);

        assertThat(filtro.periodo()).isNull();
        assertThat(filtro.anioCorte()).isNull();
        assertThat(filtro.fechaCorte()).isNull();
        assertThat(filtro.tipoUnidad()).isNull();
        assertThat(filtro).extracting(ReporteFiltro::idUnidad, ReporteFiltro::idCampus, ReporteFiltro::idSemillero)
                .containsExactly(1L, 2L, 3L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"25", "2025-3", "2025-01", "abc"})
    @DisplayName("de: rechaza períodos mal formados")
    void de_periodoInvalido(String periodo) {
        assertThatThrownBy(() -> ReporteFiltro.de(periodo, null, null, null, null))
                .isInstanceOf(FiltroReporteInvalidoException.class);
    }

    @Test
    @DisplayName("de: acepta tipos de unidad sin importar mayúsculas y rechaza desconocidos")
    void de_tipoUnidad() {
        assertThat(ReporteFiltro.de(null, "escuela", null, null, null).tipoUnidad()).isEqualTo(TipoUnidad.ESCUELA);
        assertThatThrownBy(() -> ReporteFiltro.de(null, "OTRA", null, null, null))
                .isInstanceOf(FiltroReporteInvalidoException.class);
        assertThatThrownBy(() -> ReporteFiltro.de(null, "DEPARTAMENTO", null, null, null))
                .isInstanceOf(FiltroReporteInvalidoException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "2025,   2024,   2024-12-31",
            "2025-1, 2024-2, 2024-12-31",
            "2025-2, 2025-1, 2025-06-30"
    })
    @DisplayName("periodoAnterior: año, semestre anterior (RN4)")
    void periodoAnterior_conPeriodo(String periodo, String anterior, LocalDate corte) {
        ReporteFiltro previo = ReporteFiltro.de(periodo, null, null, null, null).periodoAnterior(LocalDate.of(2026, 1, 1));

        assertThat(previo.periodo()).isEqualTo(anterior);
        assertThat(previo.fechaCorte()).isEqualTo(corte);
    }

    @ParameterizedTest
    @CsvSource({
            "2025,   2025-01-01",
            "2025-1, 2025-01-01",
            "2025-2, 2025-07-01"
    })
    @DisplayName("inicioPeriodo: primer día del año o semestre para datos fechados")
    void inicioPeriodo(String periodo, LocalDate inicio) {
        assertThat(ReporteFiltro.de(periodo, null, null, null, null).inicioPeriodo()).isEqualTo(inicio);
        assertThat(ReporteFiltro.de(null, null, null, null, null).inicioPeriodo()).isNull();
    }

    @Test
    @DisplayName("periodoAnterior: sin período compara con el mes anterior")
    void periodoAnterior_sinPeriodo() {
        ReporteFiltro previo = ReporteFiltro.de(null, null, null, null, null).periodoAnterior(LocalDate.of(2026, 1, 15));

        assertThat(previo.periodo()).isNull();
        assertThat(previo.fechaCorte()).isEqualTo(LocalDate.of(2025, 12, 15));
        assertThat(previo.anioCorte()).isEqualTo(2025);
    }

    @Test
    @DisplayName("alcances: coordinador y público conservan los filtros")
    void alcances() {
        ReporteFiltro base = ReporteFiltro.de("2025", "FACULTAD", 1L, 2L, 3L);

        ReporteFiltro coordinador = base.paraCoordinador(9L);
        assertThat(coordinador.alcance()).isEqualTo(AlcanceReporte.COORDINADOR);
        assertThat(coordinador.idCoordinador()).isEqualTo(9L);
        assertThat(base.paraPublico().alcance()).isEqualTo(AlcanceReporte.PUBLICO);
        assertThat(base.sinSemillero().idSemillero()).isNull();
        assertThat(base.sinPeriodo().periodo()).isNull();
        assertThat(base.sinPeriodo().tipoUnidad()).isEqualTo(TipoUnidad.FACULTAD);
    }

    @ParameterizedTest
    @CsvSource({
            "Facultad de Artes,          FACULTAD",
            "Escuela de Idiomas,         ESCUELA",
            "Instituto de Filosofía,     INSTITUTO",
            "Corporación Académica Ambiental, CORPORACION",
            "Seccional Apartadó,         SECCIONAL",
            "Centro de Extensión,        OTRA"
    })
    @DisplayName("TipoUnidad.desdeNombre: deriva el tipo del nombre oficial")
    void tipoUnidad_desdeNombre(String nombre, TipoUnidad tipo) {
        assertThat(TipoUnidad.desdeNombre(nombre)).isEqualTo(tipo);
    }

    @Test
    @DisplayName("TipoUnidad.desdeNombre: nombre null es OTRA")
    void tipoUnidad_nombreNull() {
        assertThat(TipoUnidad.desdeNombre(null)).isEqualTo(TipoUnidad.OTRA);
    }

    @Test
    @DisplayName("OrdenRendimiento y FormatoExportacion: parsean valores y rechazan desconocidos")
    void ordenYFormato() {
        assertThat(OrdenRendimiento.de(null)).isEqualTo(OrdenRendimiento.NOMBRE);
        assertThat(OrdenRendimiento.de("participantes")).isEqualTo(OrdenRendimiento.PARTICIPANTES);
        assertThatThrownBy(() -> OrdenRendimiento.de("cedula")).isInstanceOf(FiltroReporteInvalidoException.class);
        assertThat(FormatoExportacion.de("csv")).isEqualTo(FormatoExportacion.CSV);
        assertThatThrownBy(() -> FormatoExportacion.de("docx")).isInstanceOf(FiltroReporteInvalidoException.class);
        assertThatThrownBy(() -> FormatoExportacion.de(null)).isInstanceOf(FiltroReporteInvalidoException.class);
    }

    @Test
    @DisplayName("ReporteArchivo: compara el contenido del arreglo y no lo expone en toString")
    void reporteArchivo_equalsPorContenido() {
        ReporteArchivo uno = new ReporteArchivo("a.csv", "text/csv", new byte[]{1, 2});
        ReporteArchivo igual = new ReporteArchivo("a.csv", "text/csv", new byte[]{1, 2});

        assertThat(uno).isEqualTo(igual).hasSameHashCodeAs(igual);
        assertThat(uno).isNotEqualTo(new ReporteArchivo("a.csv", "text/csv", new byte[]{3}));
        assertThat(uno).hasToString("ReporteArchivo[nombre=a.csv, contentType=text/csv, bytes=2]");
    }
}
