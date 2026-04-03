package co.udea.semilleros.infrastructure.adapter.out.email;

import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.port.out.NotificacionEmailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionEmailAdapter implements NotificacionEmailPort {

    private final JavaMailSender mailSender;

    @Async
    @Override
    public void notificarNuevaInscripcion(Inscripcion inscripcion, String correoCoordinador) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(correoCoordinador);
            mensaje.setSubject("Nueva solicitud de inscripción - " + inscripcion.getNombreSemillero());
            mensaje.setText(construirCuerpoInscripcion(inscripcion));
            mailSender.send(mensaje);
            log.info("Notificación de inscripción enviada al coordinador: {}", correoCoordinador);
        } catch (Exception e) {
            log.error("Error al enviar notificación de inscripción al coordinador {}: {}",
                    correoCoordinador, e.getMessage());
        }
    }

    @Async
    @Override
    public void notificarFinalizacionCaracterizacion(Semillero semillero, String correoAdministrador) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(correoAdministrador);
            mensaje.setSubject("Semillero caracterizado - " + semillero.getNombre());
            mensaje.setText(construirCuerpoCaracterizacion(semillero));
            mailSender.send(mensaje);
            log.info("Notificación de caracterización enviada al administrador: {}", correoAdministrador);
        } catch (Exception e) {
            log.error("Error al enviar notificación de caracterización al administrador {}: {}",
                    correoAdministrador, e.getMessage());
        }
    }

    private String construirCuerpoInscripcion(Inscripcion inscripcion) {
        return String.format("""
                Estimado/a coordinador/a,
                
                Ha recibido una nueva solicitud de inscripción para el semillero "%s".
                
                Datos del estudiante:
                - Nombre: %s %s
                - Correo: %s
                - Cédula: %s
                - Teléfono: %s
                - Programa: %s
                - Semestre: %s
                
                Motivación:
                %s
                
                Por favor ingrese al sistema para revisar y gestionar esta solicitud.
                
                Sistema de Semilleros - Universidad de Antioquia
                """,
                inscripcion.getNombreSemillero(),
                inscripcion.getNombres(),
                inscripcion.getApellidos(),
                inscripcion.getCorreo(),
                inscripcion.getCedula(),
                inscripcion.getTelefono(),
                inscripcion.getPrograma(),
                inscripcion.getSemestre(),
                inscripcion.getMotivacion()
        );
    }

    private String construirCuerpoCaracterizacion(Semillero semillero) {
        return String.format("""
                Estimado/a administrador/a,
                
                El semillero "%s" (código: %s) ha completado su proceso de caracterización.
                
                Por favor ingrese al sistema para revisar y aprobar la información registrada.
                
                Sistema de Semilleros - Universidad de Antioquia
                """,
                semillero.getNombre(),
                semillero.getCodigo()
        );
    }
}
