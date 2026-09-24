package co.udea.semilleros.infrastructure.adapter.in.rest.sse;

import co.udea.semilleros.domain.port.in.ConsultarReportesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Avisa a los tableros de reportes abiertos cuando cambian los datos (HU13, RN46).
 * Cada intervalo configurable (RN47) compara una huella de los datos; si cambió,
 * envía el evento {@value #EVENTO_ACTUALIZADO}. Si no, envía un comentario de
 * latido para mantener viva la conexión a través de proxies.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportesEventosPublisher {

    static final String EVENTO_CONECTADO = "conectado";
    static final String EVENTO_ACTUALIZADO = "datos-actualizados";
    private static final long DURACION_CONEXION_MS = Duration.ofMinutes(30).toMillis();

    private final ConsultarReportesUseCase consultarReportesUseCase;
    private final List<SseEmitter> emisores = new CopyOnWriteArrayList<>();
    private volatile String ultimaHuella;

    @Value("${app.reportes.eventos.intervalo-ms:60000}")
    private long intervaloMs;

    public SseEmitter suscribir() {
        SseEmitter emisor = crearEmisor();
        emisores.add(emisor);
        emisor.onCompletion(() -> emisores.remove(emisor));
        emisor.onTimeout(() -> emisores.remove(emisor));
        emisor.onError(error -> emisores.remove(emisor));
        enviar(emisor, SseEmitter.event().name(EVENTO_CONECTADO).data(Map.of("intervaloMs", intervaloMs)));
        return emisor;
    }

    SseEmitter crearEmisor() {
        return new SseEmitter(DURACION_CONEXION_MS);
    }

    int suscriptores() {
        return emisores.size();
    }

    @Scheduled(fixedDelayString = "${app.reportes.eventos.intervalo-ms:60000}",
            initialDelayString = "${app.reportes.eventos.intervalo-ms:60000}")
    public void verificarCambios() {
        if (emisores.isEmpty()) {
            // Sin tableros abiertos no se consulta la base; se toma una huella nueva al reconectar
            ultimaHuella = null;
            return;
        }
        String huella;
        try {
            huella = consultarReportesUseCase.huellaDatos();
        } catch (RuntimeException e) {
            // RN48: un fallo al verificar no afecta los datos que el usuario ya ve
            log.warn("No se pudo verificar cambios en los datos de reportes: {}", e.getMessage());
            return;
        }
        boolean cambio = ultimaHuella != null && !Objects.equals(ultimaHuella, huella);
        ultimaHuella = huella;
        SseEmitter.SseEventBuilder evento = cambio
                ? SseEmitter.event().name(EVENTO_ACTUALIZADO).data(Map.of("mensaje", "Los datos han sido actualizados"))
                : SseEmitter.event().comment("latido");
        emisores.forEach(emisor -> enviar(emisor, evento));
    }

    private void enviar(SseEmitter emisor, SseEmitter.SseEventBuilder evento) {
        try {
            emisor.send(evento);
        } catch (IOException | IllegalStateException e) {
            // Conexión cerrada por el cliente
            emisores.remove(emisor);
            emisor.completeWithError(e);
        }
    }
}
