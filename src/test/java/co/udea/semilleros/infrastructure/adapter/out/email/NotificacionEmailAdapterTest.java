package co.udea.semilleros.infrastructure.adapter.out.email;

import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.model.Semillero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("NotificacionEmailAdapter - Pruebas unitarias")
class NotificacionEmailAdapterTest {

    private NotificacionEmailAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new NotificacionEmailAdapter();
        // Sin API Key de SendGrid configurada (comportamiento por defecto en dev/test):
        // el adapter debe registrar un warning y retornar sin intentar red real.
        ReflectionTestUtils.setField(adapter, "sendGridApiKey", "");
        ReflectionTestUtils.setField(adapter, "mailFrom", "noreply@udea.edu.co");
        ReflectionTestUtils.setField(adapter, "mailFromName", "Sistema de Semilleros UdeA");
    }

    // ─── notificarNuevaInscripcion ─────────────────────────────────────────────

    @Test
    @DisplayName("notificarNuevaInscripcion: no debe lanzar excepción cuando no hay API Key configurada")
    void notificarNuevaInscripcion_sinApiKey_noLanzaExcepcion() {
        // ARRANGE
        Inscripcion inscripcion = Inscripcion.builder()
                .idSemillero(1L)
                .nombreSemillero("Semillero IA")
                .nombres("Juan")
                .apellidos("Pérez")
                .cedula("1040123456")
                .correo("juan.perez@udea.edu.co")
                .telefono("3001234567")
                .programa("Ingeniería de Sistemas")
                .semestre("6")
                .motivacion("Quiero aprender investigación aplicada")
                .estado(Inscripcion.EstadoInscripcion.PENDIENTE)
                .build();

        // ACT & ASSERT
        assertThatCode(() -> adapter.notificarNuevaInscripcion(inscripcion, "coordinador@udea.edu.co"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("notificarNuevaInscripcion: no debe lanzar excepción cuando los campos opcionales están vacíos")
    void notificarNuevaInscripcion_conCamposOpcionalesVacios_noLanzaExcepcion() {
        // ARRANGE: programa/semestre/motivacion nulos ejercitan la rama "valorODefecto"
        Inscripcion inscripcion = Inscripcion.builder()
                .idSemillero(1L)
                .nombreSemillero("Semillero IA")
                .nombres("Ana")
                .apellidos("Gómez")
                .cedula("1040999999")
                .correo("ana.gomez@udea.edu.co")
                .telefono("3009876543")
                .estado(Inscripcion.EstadoInscripcion.PENDIENTE)
                .build();

        // ACT & ASSERT
        assertThatCode(() -> adapter.notificarNuevaInscripcion(inscripcion, "coordinador@udea.edu.co"))
                .doesNotThrowAnyException();
    }

    // ─── notificarFinalizacionCaracterizacion ──────────────────────────────────

    @Test
    @DisplayName("notificarFinalizacionCaracterizacion: no debe lanzar excepción cuando no hay API Key configurada")
    void notificarFinalizacionCaracterizacion_sinApiKey_noLanzaExcepcion() {
        // ARRANGE
        Semillero semillero = Semillero.builder()
                .id(1L)
                .codigo("SEM-UDEA-0001")
                .nombre("Semillero de Robótica")
                .estado(Semillero.EstadoSemillero.ACTIVO)
                .build();

        // ACT & ASSERT
        assertThatCode(() ->
                adapter.notificarFinalizacionCaracterizacion(semillero, "admin@udea.edu.co"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("notificarFinalizacionCaracterizacion: no debe lanzar excepción cuando la API Key es null")
    void notificarFinalizacionCaracterizacion_conApiKeyNula_noLanzaExcepcion() {
        // ARRANGE
        ReflectionTestUtils.setField(adapter, "sendGridApiKey", null);
        Semillero semillero = Semillero.builder()
                .id(2L)
                .codigo("SEM-UDEA-0002")
                .nombre("Semillero de Biotecnología")
                .estado(Semillero.EstadoSemillero.ACTIVO)
                .build();

        // ACT & ASSERT
        assertThatCode(() ->
                adapter.notificarFinalizacionCaracterizacion(semillero, "admin@udea.edu.co"))
                .doesNotThrowAnyException();
    }

    // ─── Registro de coordinadores ─────────────────────────────────────────────

    @Test
    @DisplayName("Correos de solicitudes de acceso: no fallan sin API Key, incluso registrando los enlaces en desarrollo")
    void correosDeAcceso_sinApiKey_noLanzanExcepcion() {
        ReflectionTestUtils.setField(adapter, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(adapter, "registrarEnlacesSinEnvio", true);

        assertThatCode(() -> {
            adapter.enviarVerificacionSolicitud("ana@udea.edu.co", "Ana", "token+/=");
            adapter.enviarActivacionCuenta("ana@udea.edu.co", "Ana", "token", true);
            adapter.enviarActivacionCuenta("ana@udea.edu.co", "Ana", "token", false);
            adapter.notificarRechazoSolicitud("ana@udea.edu.co", "Ana", "Motivo");
            adapter.enviarResumenSolicitudesPendientes("admin@udea.edu.co", 1);
            adapter.enviarResumenSolicitudesPendientes("admin@udea.edu.co", 3);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("escapar: los datos escritos por usuarios no inyectan HTML en el correo")
    void escapar_evitaInyeccionHtml() {
        org.assertj.core.api.Assertions.assertThat(NotificacionEmailAdapter.escapar("<script>alert('x')</script>"))
                .isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;");
        org.assertj.core.api.Assertions.assertThat(NotificacionEmailAdapter.escapar(null)).isEmpty();
    }
}
