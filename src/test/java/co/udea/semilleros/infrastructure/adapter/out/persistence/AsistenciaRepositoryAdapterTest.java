package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.asistencia.ConteoAsistencia;
import co.udea.semilleros.domain.model.asistencia.DatosSesion;
import co.udea.semilleros.domain.model.asistencia.EstadoAsistencia;
import co.udea.semilleros.domain.model.asistencia.IntegranteAsistencia;
import co.udea.semilleros.domain.model.asistencia.Sesion;
import co.udea.semilleros.domain.model.asistencia.SesionDetalle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static co.udea.semilleros.domain.model.asistencia.EstadoAsistencia.AUSENTE;
import static co.udea.semilleros.domain.model.asistencia.EstadoAsistencia.EXCUSADO;
import static co.udea.semilleros.domain.model.asistencia.EstadoAsistencia.PRESENTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("AsistenciaRepositoryAdapter - SQL sobre H2 (modo PostgreSQL)")
class AsistenciaRepositoryAdapterTest {

    private DriverManagerDataSource dataSource;
    private AsistenciaRepositoryAdapter adapter;

    @BeforeEach
    void crearBaseDeDatos() {
        dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:asistencia-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("asistencia/esquema-y-datos.sql")).execute(dataSource);
        adapter = new AsistenciaRepositoryAdapter(new NamedParameterJdbcTemplate(dataSource));
    }

    @AfterEach
    void cerrarBaseDeDatos() {
        new NamedParameterJdbcTemplate(dataSource).getJdbcTemplate().execute("SHUTDOWN");
    }

    private static DatosSesion datos(String titulo, LocalDate fecha, Long actividad, Object... pares) {
        List<DatosSesion.Registro> registros = new java.util.ArrayList<>();
        for (int i = 0; i < pares.length; i += 2) {
            registros.add(new DatosSesion.Registro((Long) pares[i], (EstadoAsistencia) pares[i + 1]));
        }
        return new DatosSesion(titulo, fecha, actividad, registros);
    }

    @Test
    @DisplayName("integrantes activos, catálogo de actividades y lista de la sesión")
    void consultasAuxiliares() {
        assertThat(adapter.idsIntegrantesActivos(1L)).containsExactly(1L, 2L);
        assertThat(adapter.existeActividad(2L)).isTrue();
        assertThat(adapter.existeActividad(9L)).isFalse();
        Long id = adapter.crearSesion(1L, datos("Taller", LocalDate.of(2026, 3, 1), 2L, 1L, PRESENTE, 2L, AUSENTE));
        assertThat(adapter.idsIntegrantesDeSesion(id)).containsExactlyInAnyOrder(1L, 2L);
        assertThat(adapter.semilleroDeSesion(id)).contains(1L);
        assertThat(adapter.semilleroDeSesion(999L)).isEmpty();
    }

    @Test
    @DisplayName("crearSesion y obtenerSesion: guardan la sesión con su lista y conteo por estado")
    void crearYObtener() {
        Long id = adapter.crearSesion(1L, datos("  Club de revista ", LocalDate.of(2026, 2, 10), 1L,
                1L, PRESENTE, 2L, EXCUSADO));

        SesionDetalle detalle = adapter.obtenerSesion(id).orElseThrow();

        assertThat(detalle.sesion()).extracting(Sesion::titulo, Sesion::actividad, Sesion::fecha, Sesion::idActividad)
                .containsExactly("Club de revista", "Seminarios", LocalDate.of(2026, 2, 10), 1L);
        assertThat(detalle.sesion().asistencia()).isEqualTo(new ConteoAsistencia(1, 0, 1));
        assertThat(detalle.asistencias())
                .extracting(SesionDetalle.Asistente::nombre, SesionDetalle.Asistente::estado)
                .containsExactly(tuple("Beto Arias", EXCUSADO), tuple("Ana Zapata", PRESENTE));
        assertThat(adapter.obtenerSesion(999L)).isEmpty();
    }

    @Test
    @DisplayName("actualizarSesion: reemplaza datos y lista; eliminarSesion borra la sesión y su asistencia")
    void actualizarYEliminar() {
        Long id = adapter.crearSesion(1L, datos("Reunión", LocalDate.of(2026, 2, 10), 1L, 1L, PRESENTE, 2L, PRESENTE));

        adapter.actualizarSesion(id, datos("Reunión de cierre", LocalDate.of(2026, 2, 11), null, 1L, AUSENTE));

        Sesion sesion = adapter.obtenerSesion(id).orElseThrow().sesion();
        assertThat(sesion.titulo()).isEqualTo("Reunión de cierre");
        assertThat(sesion.idActividad()).isNull();
        assertThat(sesion.actividad()).isNull();
        assertThat(sesion.asistencia()).isEqualTo(new ConteoAsistencia(0, 1, 0));

        adapter.eliminarSesion(id);
        assertThat(adapter.obtenerSesion(id)).isEmpty();
        assertThat(adapter.idsIntegrantesDeSesion(id)).isEmpty();
    }

    @Test
    @DisplayName("listarSesiones: filtra por semillero y rango de fechas, de la más reciente a la más antigua")
    void listarSesiones() {
        adapter.crearSesion(1L, datos("Enero", LocalDate.of(2026, 1, 20), 1L, 1L, PRESENTE));
        adapter.crearSesion(1L, datos("Agosto", LocalDate.of(2026, 8, 5), 2L, 1L, AUSENTE, 2L, PRESENTE));
        adapter.crearSesion(2L, datos("Otro semillero", LocalDate.of(2026, 8, 6), 2L, 4L, PRESENTE));

        assertThat(adapter.listarSesiones(1L, null, null)).extracting(Sesion::titulo).containsExactly("Agosto", "Enero");
        assertThat(adapter.listarSesiones(1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31)))
                .singleElement().extracting(Sesion::asistencia).isEqualTo(new ConteoAsistencia(1, 1, 0));
        assertThat(adapter.listarSesiones(1L, null, LocalDate.of(2026, 6, 30))).extracting(Sesion::titulo).containsExactly("Enero");
    }

    @Test
    @DisplayName("asistenciaPorIntegrante: activos y retirados con asistencia en el rango, con su conteo")
    void asistenciaPorIntegrante() {
        adapter.crearSesion(1L, datos("S1", LocalDate.of(2026, 1, 20), 1L, 1L, PRESENTE, 2L, AUSENTE, 3L, PRESENTE));
        adapter.crearSesion(1L, datos("S2", LocalDate.of(2026, 8, 5), 1L, 1L, EXCUSADO, 2L, PRESENTE));

        List<IntegranteAsistencia> todos = adapter.asistenciaPorIntegrante(1L, null, null);
        assertThat(todos).extracting(IntegranteAsistencia::nombre, IntegranteAsistencia::activo, IntegranteAsistencia::asistencia)
                .containsExactly(
                        tuple("Beto Arias", true, new ConteoAsistencia(1, 1, 0)),
                        tuple("Carla Mejía", false, new ConteoAsistencia(1, 0, 0)),
                        tuple("Ana Zapata", true, new ConteoAsistencia(1, 0, 1)));

        // En el segundo semestre la integrante retirada no tiene asistencia y no aparece
        assertThat(adapter.asistenciaPorIntegrante(1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31)))
                .extracting(IntegranteAsistencia::nombre).containsExactly("Beto Arias", "Ana Zapata");
    }
}
