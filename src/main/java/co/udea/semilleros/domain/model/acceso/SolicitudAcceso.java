package co.udea.semilleros.domain.model.acceso;

import lombok.Builder;
import lombok.With;

import java.time.Instant;

/**
 * Solicitud de una persona para obtener una cuenta de coordinador.
 * {@code tokenHash} es el hash SHA-256 del enlace de verificación de correo.
 */
@Builder(toBuilder = true)
@With
public record SolicitudAcceso(
        Long id,
        String nombres,
        String apellidos,
        String cedula,
        String correo,
        Long idUnidadAcademica,
        String unidadAcademica,
        String justificacion,
        EstadoSolicitud estado,
        String tokenHash,
        Instant tokenExpira,
        int enviosVerificacion,
        Instant ultimoEnvio,
        String ipOrigen,
        Instant fechaCreacion,
        Instant fechaVerificacion,
        Long idRevisor,
        Instant fechaRevision,
        String motivoRechazo,
        boolean bloqueada
) {
    public String nombreCompleto() {
        return (nombres + " " + apellidos).trim();
    }
}
