package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.reporte.OrdenRendimiento;
import co.udea.semilleros.domain.model.reporte.ReporteConteo;
import co.udea.semilleros.domain.model.reporte.ReporteFiltro;
import co.udea.semilleros.domain.model.reporte.ReporteOpcion;
import co.udea.semilleros.domain.model.reporte.ReporteRendimiento;
import co.udea.semilleros.domain.model.reporte.ReporteUnidad;
import co.udea.semilleros.domain.model.reporte.TipoUnidad;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("ReportesRepositoryAdapter - Consultas SQL sobre H2 (modo PostgreSQL)")
class ReportesRepositoryAdapterTest {

    private DriverManagerDataSource dataSource;
    private ReportesRepositoryAdapter adapter;

    @BeforeEach
    void crearBaseDeDatos() {
        dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:reportes-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("reportes/esquema-y-datos.sql")).execute(dataSource);
        adapter = new ReportesRepositoryAdapter(new NamedParameterJdbcTemplate(dataSource));
    }

    @AfterEach
    void cerrarBaseDeDatos() {
        new NamedParameterJdbcTemplate(dataSource).getJdbcTemplate().execute("SHUTDOWN");
    }

    private static ReporteFiltro todos() {
        return ReporteFiltro.de(null, null, null, null, null);
    }

    @Test
    @DisplayName("KPIs: cuentan semilleros activos, personas únicas y actividades realizadas (RN2)")
    void kpis_sinFiltros() {
        assertThat(adapter.contarSemillerosActivos(todos())).isEqualTo(4);
        assertThat(adapter.contarIntegrantesRegistrados(todos())).isEqualTo(4);
        assertThat(adapter.contarIntegrantesActivos(todos())).isEqualTo(3);
        assertThat(adapter.contarActividadesRealizadas(todos())).isEqualTo(4);
    }

    @Test
    @DisplayName("KPIs: el período limita el año de creación y la fecha de ingreso")
    void kpis_conPeriodo() {
        ReporteFiltro filtro = ReporteFiltro.de("2024-1", null, null, null, null);

        assertThat(adapter.contarSemillerosActivos(filtro)).isEqualTo(3);
        assertThat(adapter.contarIntegrantesRegistrados(filtro)).isEqualTo(2);
        assertThat(adapter.contarIntegrantesActivos(filtro)).isEqualTo(1);
    }

    @Test
    @DisplayName("Filtros: tipo de unidad (incluye nombres con tilde), unidad, campus, semillero y coordinador")
    void filtros_combinados() {
        assertThat(adapter.contarSemillerosActivos(ReporteFiltro.de(null, "FACULTAD", null, null, null))).isEqualTo(2);
        assertThat(adapter.contarSemillerosActivos(ReporteFiltro.de(null, "CORPORACION", null, null, null))).isEqualTo(1);
        assertThat(adapter.contarSemillerosActivos(ReporteFiltro.de(null, null, 2L, null, null))).isEqualTo(1);
        assertThat(adapter.contarSemillerosActivos(ReporteFiltro.de(null, null, null, 2L, null))).isEqualTo(1);
        assertThat(adapter.contarSemillerosActivos(ReporteFiltro.de(null, null, null, null, 1L))).isEqualTo(1);
        assertThat(adapter.contarSemillerosActivos(todos().paraCoordinador(10L))).isEqualTo(2);
    }

    @Test
    @DisplayName("distribucionPorUnidad: semilleros y estudiantes activos únicos por unidad (HU3)")
    void distribucionPorUnidad() {
        assertThat(adapter.distribucionPorUnidad(todos()))
                .extracting(ReporteUnidad::nombre, ReporteUnidad::tipo, ReporteUnidad::semilleros, ReporteUnidad::estudiantes)
                .containsExactly(
                        tuple("Facultad de Ingeniería", TipoUnidad.FACULTAD, 2L, 2L),
                        tuple("Corporación Académica Ambiental", TipoUnidad.CORPORACION, 1L, 0L),
                        tuple("Escuela de Idiomas", TipoUnidad.ESCUELA, 1L, 1L));
    }

    @Test
    @DisplayName("distribucionPorUnidad: con período cuenta solo integrantes vinculados hasta el corte")
    void distribucionPorUnidad_conPeriodo() {
        assertThat(adapter.distribucionPorUnidad(ReporteFiltro.de("2024", null, null, null, null)))
                .extracting(ReporteUnidad::nombre, ReporteUnidad::estudiantes)
                .containsExactly(tuple("Facultad de Ingeniería", 1L), tuple("Corporación Académica Ambiental", 0L));
    }

    @Test
    @DisplayName("distribucionPorCampus: incluye sedes sin semilleros, de mayor a menor (HU4)")
    void distribucionPorCampus() {
        assertThat(adapter.distribucionPorCampus(todos()))
                .extracting(ReporteConteo::id, ReporteConteo::nombre, ReporteConteo::cantidad)
                .containsExactly(tuple("1", "Medellín", 3L), tuple("2", "Apartadó", 1L), tuple("3", "Caucasia", 0L));
    }

    @Test
    @DisplayName("integrantesPorSexo: personas activas únicas, siempre con Femenino y Masculino (HU5)")
    void integrantesPorSexo() {
        assertThat(adapter.integrantesPorSexo(todos()))
                .extracting(ReporteConteo::id, ReporteConteo::cantidad)
                .containsExactly(tuple("FEMENINO", 1L), tuple("MASCULINO", 1L), tuple("OTRO", 1L));
        assertThat(adapter.integrantesPorSexo(ReporteFiltro.de(null, "CORPORACION", null, null, null)))
                .extracting(ReporteConteo::nombre, ReporteConteo::cantidad)
                .containsExactly(tuple("Femenino", 0L), tuple("Masculino", 0L));
    }

    @Test
    @DisplayName("integrantesPorSexo: los integrantes sin sexo registrado aparecen como No informado")
    void integrantesPorSexo_noInformado() {
        new NamedParameterJdbcTemplate(dataSource).getJdbcTemplate()
                .update("UPDATE semillero_integrante SET activo = TRUE WHERE id = 4");

        assertThat(adapter.integrantesPorSexo(todos()))
                .extracting(ReporteConteo::id, ReporteConteo::nombre, ReporteConteo::cantidad)
                .contains(tuple("NO_INFORMADO", "No informado", 1L));
    }

    @Test
    @DisplayName("integrantesPorRol: asignaciones activas por rol del catálogo, en su orden (RN17)")
    void integrantesPorRol() {
        assertThat(adapter.integrantesPorRol(todos()))
                .extracting(ReporteConteo::nombre, ReporteConteo::cantidad)
                .containsExactly(
                        tuple("Estudiante Investigador", 2L),
                        tuple("Auxiliar", 1L),
                        tuple("Coordinador", 0L),
                        tuple("Tutor", 1L),
                        tuple("Semillerista Junior", 0L));
    }

    @Test
    @DisplayName("semillerosCreadosPorAnio: ignora semilleros sin año de creación (HU7)")
    void semillerosCreadosPorAnio() {
        assertThat(adapter.semillerosCreadosPorAnio(todos())).isEqualTo(Map.of(2022, 1L, 2024, 1L, 2025, 1L));
    }

    @Test
    @DisplayName("actividadesPorTipo: semilleros que realizan cada actividad del catálogo (HU8)")
    void actividadesPorTipo() {
        assertThat(adapter.actividadesPorTipo(todos()))
                .extracting(ReporteConteo::nombre, ReporteConteo::cantidad)
                .containsExactly(tuple("Seminarios", 1L), tuple("Talleres", 2L), tuple("Conversatorios", 1L));
        assertThat(adapter.actividadesPorTipo(ReporteFiltro.de(null, null, null, null, 2L)))
                .extracting(ReporteConteo::cantidad)
                .containsExactly(0L, 1L, 0L);
    }

    @Test
    @DisplayName("rendimiento: incluye activos e inactivos, pagina y ordena por nombre (HU9)")
    void rendimiento_paginaYOrdena() {
        PageResult<ReporteRendimiento> pagina = adapter.rendimiento(todos(), 0, 2, OrdenRendimiento.NOMBRE, true);

        assertThat(pagina.getTotalElementos()).isEqualTo(5);
        assertThat(pagina.getTotalPaginas()).isEqualTo(3);
        assertThat(pagina.isEsPrimeraPagina()).isTrue();
        assertThat(pagina.isEsUltimaPagina()).isFalse();
        assertThat(pagina.getContenido())
                .extracting(ReporteRendimiento::nombre, ReporteRendimiento::estado, ReporteRendimiento::campus)
                .containsExactly(tuple("Ambiental", "ACTIVO", "Apartadó"), tuple("Inactivo", "INACTIVO", "Medellín"));

        PageResult<ReporteRendimiento> ultima = adapter.rendimiento(todos(), 2, 2, OrdenRendimiento.NOMBRE, true);
        assertThat(ultima.isEsUltimaPagina()).isTrue();
        assertThat(ultima.getContenido()).extracting(ReporteRendimiento::nombre).containsExactly("Semillero IA");
    }

    @Test
    @DisplayName("rendimiento: ordena por participantes de forma descendente con sus métricas")
    void rendimiento_ordenaPorParticipantes() {
        assertThat(adapter.rendimientoCompleto(todos(), OrdenRendimiento.PARTICIPANTES, false))
                .extracting(ReporteRendimiento::nombre, ReporteRendimiento::participantes,
                        ReporteRendimiento::actividadesRealizadas, ReporteRendimiento::tipoUnidad)
                .containsExactly(
                        tuple("Semillero IA", 2L, 2L, TipoUnidad.FACULTAD),
                        tuple("Robótica", 1L, 1L, TipoUnidad.FACULTAD),
                        tuple("Lenguas", 1L, 1L, TipoUnidad.ESCUELA),
                        tuple("Inactivo", 1L, 1L, TipoUnidad.FACULTAD),
                        tuple("Ambiental", 0L, 0L, TipoUnidad.CORPORACION));
    }

    @Test
    @DisplayName("rendimiento: todas las columnas de orden generan SQL válido")
    void rendimiento_todasLasColumnas() {
        for (OrdenRendimiento orden : OrdenRendimiento.values()) {
            assertThat(adapter.rendimientoCompleto(todos(), orden, true)).hasSize(5);
        }
        assertThat(adapter.rendimientoCompleto(todos(), null, true)).first()
                .extracting(ReporteRendimiento::nombre).isEqualTo("Ambiental");
    }

    @Test
    @DisplayName("rendimiento: sin resultados retorna una única página vacía")
    void rendimiento_vacio() {
        PageResult<ReporteRendimiento> pagina = adapter.rendimiento(todos().paraCoordinador(999L), 0, 10,
                OrdenRendimiento.NOMBRE, true);

        assertThat(pagina.getContenido()).isEmpty();
        assertThat(pagina.getTotalElementos()).isZero();
        assertThat(pagina.isEsUltimaPagina()).isTrue();
    }

    @Test
    @DisplayName("semillerosActivos: activos con nombre, ordenados alfabéticamente (RN49)")
    void semillerosActivos() {
        assertThat(adapter.semillerosActivos(todos()))
                .extracting(ReporteOpcion::nombre)
                .containsExactly("Ambiental", "Lenguas", "Robótica", "Semillero IA");
    }

    @Test
    @DisplayName("huellaDatos: cambia cuando cambian los datos de los reportes (HU13)")
    void huellaDatos_cambiaConLosDatos() {
        String antes = adapter.huellaDatos();

        new NamedParameterJdbcTemplate(dataSource).getJdbcTemplate()
                .update("INSERT INTO semillero_integrante VALUES (7, 1, 'F', 'FEMENINO', 'TUTOR', TRUE, NULL)");

        assertThat(antes).isNotBlank();
        assertThat(adapter.huellaDatos()).isNotEqualTo(antes);
    }
}
