package co.udea.semilleros.infrastructure.adapter.in.rest.sse;

import co.udea.semilleros.domain.port.in.ConsultarReportesUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ReportesEventosPublisher - Actualización en tiempo real (HU13)")
class ReportesEventosPublisherTest {

    /** Emisor que registra lo enviado en lugar de escribir en una respuesta HTTP. */
    static class EmisorDePrueba extends SseEmitter {
        final List<String> enviados = new ArrayList<>();
        boolean fallar;

        @Override
        public void send(SseEventBuilder evento) throws IOException {
            if (fallar) {
                throw new IOException("conexión cerrada");
            }
            enviados.add(evento.build().stream().map(d -> String.valueOf(d.getData())).collect(Collectors.joining()));
        }
    }

    private ConsultarReportesUseCase useCase;
    private List<EmisorDePrueba> creados;
    private ReportesEventosPublisher publisher;

    @BeforeEach
    void setUp() {
        useCase = mock(ConsultarReportesUseCase.class);
        creados = new ArrayList<>();
        publisher = new ReportesEventosPublisher(useCase) {
            @Override
            SseEmitter crearEmisor() {
                EmisorDePrueba emisor = new EmisorDePrueba();
                creados.add(emisor);
                return emisor;
            }
        };
    }

    @Test
    @DisplayName("suscribir: envía el evento de conexión")
    void suscribir_enviaConectado() {
        publisher.suscribir();

        assertThat(publisher.suscriptores()).isEqualTo(1);
        assertThat(creados.get(0).enviados).singleElement().asString().contains("event:conectado");
    }

    @Test
    @DisplayName("verificarCambios: sin suscriptores no consulta la base de datos")
    void verificarCambios_sinSuscriptores() {
        publisher.verificarCambios();

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("verificarCambios: notifica solo cuando la huella cambia; si no, envía latido")
    void verificarCambios_notificaCuandoCambia() {
        publisher.suscribir();
        when(useCase.huellaDatos()).thenReturn("a", "a", "b");

        publisher.verificarCambios();
        publisher.verificarCambios();
        publisher.verificarCambios();

        List<String> enviados = creados.get(0).enviados;
        assertThat(enviados).hasSize(4);
        assertThat(enviados.get(1)).contains(":latido").doesNotContain("datos-actualizados");
        assertThat(enviados.get(2)).contains(":latido");
        assertThat(enviados.get(3)).contains("event:datos-actualizados");
    }

    @Test
    @DisplayName("verificarCambios: si falla la consulta no notifica ni desconecta (RN48)")
    void verificarCambios_errorAlConsultar() {
        publisher.suscribir();
        when(useCase.huellaDatos()).thenThrow(new IllegalStateException("BD caída"));

        publisher.verificarCambios();

        assertThat(creados.get(0).enviados).hasSize(1);
        assertThat(publisher.suscriptores()).isEqualTo(1);
    }

    @Test
    @DisplayName("verificarCambios: elimina emisores cuya conexión se cerró")
    void verificarCambios_eliminaConexionesCerradas() {
        publisher.suscribir();
        creados.get(0).fallar = true;
        when(useCase.huellaDatos()).thenReturn("a");

        publisher.verificarCambios();

        assertThat(publisher.suscriptores()).isZero();
    }
}
