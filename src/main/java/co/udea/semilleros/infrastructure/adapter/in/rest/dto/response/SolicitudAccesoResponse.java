package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import co.udea.semilleros.domain.model.acceso.EstadoSolicitud;
import co.udea.semilleros.domain.model.acceso.SolicitudAcceso;

import java.time.Instant;

/**
 * Solicitud tal como la ve el administrador: sin token ni IP de origen.
 */
public record SolicitudAccesoResponse(
        Long id,
        String nombres,
        String apellidos,
        String cedula,
        String correo,
        String unidadAcademica,
        String justificacion,
        EstadoSolicitud estado,
        Instant fechaCreacion,
        Instant fechaVerificacion,
        Instant fechaRevision,
        String motivoRechazo,
        boolean bloqueada
) {
    public static SolicitudAccesoResponse de(SolicitudAcceso s) {
        return new SolicitudAccesoResponse(s.id(), s.nombres(), s.apellidos(), s.cedula(), s.correo(),
                s.unidadAcademica(), s.justificacion(), s.estado(), s.fechaCreacion(), s.fechaVerificacion(),
                s.fechaRevision(), s.motivoRechazo(), s.bloqueada());
    }
}
