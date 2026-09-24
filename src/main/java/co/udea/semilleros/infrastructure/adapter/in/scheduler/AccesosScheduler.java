package co.udea.semilleros.infrastructure.adapter.in.scheduler;

import co.udea.semilleros.domain.port.in.AdministrarAccesosUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Mantenimiento del registro de coordinadores: limpia solicitudes sin confirmar y enlaces
 * vencidos, y envía al administrador un resumen diario en lugar de un correo por solicitud.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccesosScheduler {

    private final AdministrarAccesosUseCase administrarAccesosUseCase;

    @Scheduled(cron = "${app.accesos.limpieza-cron:0 0 * * * *}", zone = "America/Bogota")
    public void limpiarVencidos() {
        int eliminados = administrarAccesosUseCase.limpiarVencidos();
        if (eliminados > 0) {
            log.info("Registro de coordinadores: {} registros vencidos eliminados", eliminados);
        }
    }

    @Scheduled(cron = "${app.accesos.resumen-cron:0 0 7 * * *}", zone = "America/Bogota")
    public void enviarResumenPendientes() {
        administrarAccesosUseCase.enviarResumenPendientes();
    }
}
