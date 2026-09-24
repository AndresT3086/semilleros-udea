package co.udea.semilleros.domain.port.in;

import co.udea.semilleros.domain.model.acceso.DatosInvitacion;
import co.udea.semilleros.domain.model.acceso.EstadoSolicitud;
import co.udea.semilleros.domain.model.acceso.InvitacionEnviada;
import co.udea.semilleros.domain.model.acceso.SolicitudAcceso;

import java.util.List;

/**
 * Gestión de accesos de coordinadores por parte del administrador.
 */
public interface AdministrarAccesosUseCase {

    List<SolicitudAcceso> listarSolicitudes(EstadoSolicitud estado);

    long contarPendientes();

    void aprobar(Long idSolicitud, Long idAdministrador);

    void rechazar(Long idSolicitud, Long idAdministrador, String motivo, boolean bloquear);

    InvitacionEnviada invitar(DatosInvitacion datos, Long idAdministrador);

    /** Mantenimiento programado: borra solicitudes sin verificar vencidas y tokens viejos. */
    int limpiarVencidos();

    /** Mantenimiento programado: avisa al administrador cuántas solicitudes esperan revisión. */
    void enviarResumenPendientes();
}
