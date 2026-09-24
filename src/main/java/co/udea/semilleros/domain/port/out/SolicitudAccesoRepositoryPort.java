package co.udea.semilleros.domain.port.out;

import co.udea.semilleros.domain.model.acceso.EstadoSolicitud;
import co.udea.semilleros.domain.model.acceso.SolicitudAcceso;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SolicitudAccesoRepositoryPort {

    /** Solicitud en curso (sin verificar o pendiente) con ese correo o esa cédula. */
    Optional<SolicitudAcceso> buscarEnCurso(String correo, String cedula);

    /** Última solicitud rechazada con ese correo o esa cédula. */
    Optional<SolicitudAcceso> buscarUltimaRechazada(String correo, String cedula);

    Optional<SolicitudAcceso> buscarPorId(Long id);

    Optional<SolicitudAcceso> buscarPorTokenHash(String tokenHash);

    Long crear(SolicitudAcceso solicitud);

    void actualizar(SolicitudAcceso solicitud);

    List<SolicitudAcceso> listarPorEstado(EstadoSolicitud estado);

    long contarPorEstado(EstadoSolicitud estado);

    /** Borra las solicitudes cuyo correo nunca se confirmó y cuyo enlace ya venció. */
    int eliminarNoVerificadasVencidas(Instant ahora);
}
