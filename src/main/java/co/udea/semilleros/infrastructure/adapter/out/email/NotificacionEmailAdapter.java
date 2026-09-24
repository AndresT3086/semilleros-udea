package co.udea.semilleros.infrastructure.adapter.out.email;

import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.port.out.NotificacionEmailPort;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class NotificacionEmailAdapter implements NotificacionEmailPort {


    @Value("${app.mail.sendgrid.api-key:}")
    private String sendGridApiKey;

    @Value("${app.mail.from:noreply@udea.edu.co}")
    private String mailFrom;

    @Value("${app.mail.from-name:Sistema de Semilleros UdeA}")
    private String mailFromName;

    /** URL pública del frontend, para los enlaces de verificación y activación. */
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Solo para desarrollo local sin SendGrid: registra en el log los enlaces que no se
     * pudieron enviar. Nunca debe activarse en producción.
     */
    @Value("${app.mail.registrar-enlaces-sin-envio:false}")
    private boolean registrarEnlacesSinEnvio;

    @Async
    @Override
    public void notificarNuevaInscripcion(Inscripcion inscripcion, String correoCoordinador) {
        String asunto = "Nueva solicitud de inscripción — " + inscripcion.getNombreSemillero();
        String cuerpo = construirCuerpoInscripcion(inscripcion);

        enviarCorreo(correoCoordinador, asunto, cuerpo);
    }

    @Async
    @Override
    public void notificarFinalizacionCaracterizacion(Semillero semillero, String correoAdministrador) {
        String asunto = "Semillero caracterizado — " + semillero.getNombre();
        String cuerpo = construirCuerpoCaracterizacion(semillero);

        enviarCorreo(correoAdministrador, asunto, cuerpo);
    }

    @Override
    public void enviarVerificacionSolicitud(String correo, String nombre, String token) {
        String enlace = enlace("verificar", token);
        enviarCorreo(correo, "Confirma tu solicitud de acceso como coordinador", plantilla(
                "Confirma tu correo",
                "<p>Hola " + escapar(nombre) + ",</p>"
                        + "<p>Recibimos tu solicitud para acceder al sistema como coordinador de semillero. "
                        + "Para que un administrador pueda revisarla, confirma que este correo es tuyo:</p>"
                        + boton(enlace, "Confirmar mi correo")
                        + "<p style=\"font-size: 13px; color: #666;\">El enlace vence en una hora. "
                        + "Si no hiciste esta solicitud, ignora este mensaje.</p>"), enlace);
    }

    @Override
    public void enviarActivacionCuenta(String correo, String nombre, String token, boolean invitacion) {
        String enlace = enlace("activar", token);
        String motivo = invitacion
                ? "Un administrador te invitó a coordinar semilleros en el sistema."
                : "Tu solicitud de acceso como coordinador fue aprobada.";
        enviarCorreo(correo, invitacion ? "Invitación al Sistema de Semilleros UdeA" : "Tu solicitud de acceso fue aprobada",
                plantilla("Crea tu contraseña",
                        "<p>Hola " + escapar(nombre) + ",</p><p>" + motivo
                                + " Para activar tu cuenta, crea tu contraseña:</p>"
                                + boton(enlace, "Crear mi contraseña")
                                + "<p style=\"font-size: 13px; color: #666;\">El enlace vence en 24 horas y solo "
                                + "se puede usar una vez.</p>"), enlace);
    }

    @Override
    public void notificarRechazoSolicitud(String correo, String nombre, String motivo) {
        enviarCorreo(correo, "Resultado de tu solicitud de acceso", plantilla("Solicitud no aprobada",
                "<p>Hola " + escapar(nombre) + ",</p>"
                        + "<p>Tu solicitud de acceso como coordinador no fue aprobada por el siguiente motivo:</p>"
                        + "<div style=\"background: #fff8e1; border-left: 4px solid #f9a825; padding: 12px 16px;\">"
                        + escapar(motivo) + "</div>"
                        + "<p>Si crees que se trata de un error, comunícate con la Vicerrectoría de Investigación.</p>"),
                null);
    }

    @Override
    public void enviarResumenSolicitudesPendientes(String correoAdministrador, long pendientes) {
        String enlace = frontendUrl + "/?vista=admin";
        enviarCorreo(correoAdministrador, "Solicitudes de acceso pendientes: " + pendientes, plantilla(
                "Solicitudes por revisar",
                "<p>Estimado/a administrador/a,</p><p>Hay <strong>" + pendientes + "</strong> "
                        + (pendientes == 1 ? "solicitud" : "solicitudes")
                        + " de acceso como coordinador con el correo confirmado, esperando revisión.</p>"
                        + boton(enlace, "Ir al panel de administración")), null);
    }

    private String enlace(String accion, String token) {
        return frontendUrl + "/?accion=" + accion + "&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private void enviarCorreo(String destinatario, String asunto, String cuerpo, String enlace) {
        if (enlace != null && registrarEnlacesSinEnvio && (sendGridApiKey == null || sendGridApiKey.isBlank())) {
            log.warn("[desarrollo] SENDGRID_API_KEY vacía: correo NO enviado a {}. Enlace: {}", destinatario, enlace);
        }
        enviarCorreo(destinatario, asunto, cuerpo);
    }

    private static String boton(String enlace, String texto) {
        return "<p style=\"text-align: center; margin: 28px 0;\"><a href=\"" + escapar(enlace) + "\" "
                + "style=\"background: #3d5a1e; color: white; padding: 12px 24px; border-radius: 6px; "
                + "text-decoration: none; font-weight: bold;\">" + texto + "</a></p>"
                + "<p style=\"font-size: 12px; color: #777; word-break: break-all;\">Si el botón no funciona, copia este "
                + "enlace en tu navegador:<br>" + escapar(enlace) + "</p>";
    }

    private static String plantilla(String titulo, String contenido) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
                  <div style="background-color: #3d5a1e; padding: 20px; text-align: center;">
                    <h1 style="color: white; margin: 0; font-size: 20px;">Sistema de Semilleros de Investigación</h1>
                    <p style="color: #cde; margin: 5px 0 0 0; font-size: 14px;">Universidad de Antioquia</p>
                  </div>
                  <div style="padding: 30px 20px;">
                    <h2 style="color: #3d5a1e; font-size: 18px;">%s</h2>
                    %s
                  </div>
                  <div style="background: #f0f0f0; padding: 15px 20px; text-align: center; font-size: 12px; color: #777;">
                    Sistema de Semilleros — Universidad de Antioquia<br>
                    Este es un mensaje automático, por favor no responder a este correo.
                  </div>
                </body>
                </html>
                """.formatted(titulo, contenido);
    }

    /** Los datos escritos por usuarios se escapan para que no inyecten HTML en el correo. */
    static String escapar(String texto) {
        return texto == null ? "" : HtmlUtils.htmlEscape(texto);
    }

    private void enviarCorreo(String destinatario, String asunto, String cuerpo) {
        if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
            log.warn("SendGrid API Key no configurada. Correo NO enviado a: {}", destinatario);
            return;
        }

        try {
            Email from    = new Email(mailFrom, mailFromName);
            Email to      = new Email(destinatario);
            Content content = new Content("text/html", cuerpo);
            Mail mail     = new Mail(from, asunto, to, content);

            SendGrid sg      = new SendGrid(sendGridApiKey);
            Request request  = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Correo enviado exitosamente a: {} | Status: {}",
                        destinatario, response.getStatusCode());
            } else {
                log.error("Error al enviar correo a: {} | Status: {} | Body: {}",
                        destinatario, response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("Excepción al enviar correo a {}: {}", destinatario, e.getMessage());
        }
    }

    private String construirCuerpoInscripcion(Inscripcion inscripcion) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
                
                  <div style="background-color: #3d5a1e; padding: 20px; text-align: center;">
                    <h1 style="color: white; margin: 0; font-size: 20px;">
                      Sistema de Semilleros de Investigación
                    </h1>
                    <p style="color: #cde; margin: 5px 0 0 0; font-size: 14px;">
                      Universidad de Antioquia
                    </p>
                  </div>
                
                  <div style="padding: 30px 20px;">
                    <h2 style="color: #3d5a1e; font-size: 18px;">
                      Nueva solicitud de inscripción
                    </h2>
                    <p>Estimado/a coordinador/a,</p>
                    <p>
                      Ha recibido una nueva solicitud de ingreso al semillero
                      <strong>%s</strong>.
                    </p>
                
                    <div style="background: #f5f5f5; border-left: 4px solid #3d5a1e;
                                padding: 15px 20px; margin: 20px 0; border-radius: 4px;">
                      <h3 style="margin: 0 0 12px 0; color: #3d5a1e; font-size: 15px;">
                        Datos del estudiante
                      </h3>
                      <table style="width: 100%%; border-collapse: collapse; font-size: 14px;">
                        <tr>
                          <td style="padding: 4px 8px; font-weight: bold; width: 140px;">Nombre:</td>
                          <td style="padding: 4px 8px;">%s %s</td>
                        </tr>
                        <tr style="background:#ebebeb;">
                          <td style="padding: 4px 8px; font-weight: bold;">Correo:</td>
                          <td style="padding: 4px 8px;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding: 4px 8px; font-weight: bold;">Cédula:</td>
                          <td style="padding: 4px 8px;">%s</td>
                        </tr>
                        <tr style="background:#ebebeb;">
                          <td style="padding: 4px 8px; font-weight: bold;">Teléfono:</td>
                          <td style="padding: 4px 8px;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding: 4px 8px; font-weight: bold;">Programa:</td>
                          <td style="padding: 4px 8px;">%s</td>
                        </tr>
                        <tr style="background:#ebebeb;">
                          <td style="padding: 4px 8px; font-weight: bold;">Semestre:</td>
                          <td style="padding: 4px 8px;">%s</td>
                        </tr>
                      </table>
                    </div>
                
                    <div style="background: #fff8e1; border-left: 4px solid #f9a825;
                                padding: 15px 20px; margin: 20px 0; border-radius: 4px;">
                      <h3 style="margin: 0 0 8px 0; font-size: 14px; color: #555;">
                        Motivación del estudiante
                      </h3>
                      <p style="margin: 0; font-size: 14px; color: #444;">%s</p>
                    </div>
                
                    <p style="font-size: 14px;">
                      Por favor ingrese al sistema para revisar y gestionar esta solicitud.
                      La solicitud quedó en estado <strong>PENDIENTE</strong>.
                    </p>
                  </div>
                
                  <div style="background: #f0f0f0; padding: 15px 20px; text-align: center;
                              font-size: 12px; color: #777;">
                    Sistema de Semilleros — Universidad de Antioquia<br>
                    Este es un mensaje automático, por favor no responder a este correo.
                  </div>
                
                </body>
                </html>
                """.formatted(
                inscripcion.getNombreSemillero(),
                inscripcion.getNombres(),
                inscripcion.getApellidos(),
                inscripcion.getCorreo(),
                inscripcion.getCedula(),
                inscripcion.getTelefono(),
                valorODefecto(inscripcion.getPrograma()),
                valorODefecto(inscripcion.getSemestre()),
                valorODefecto(inscripcion.getMotivacion())
        );
    }

    private String construirCuerpoCaracterizacion(Semillero semillero) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
                
                  <div style="background-color: #3d5a1e; padding: 20px; text-align: center;">
                    <h1 style="color: white; margin: 0; font-size: 20px;">
                      Sistema de Semilleros de Investigación
                    </h1>
                    <p style="color: #cde; margin: 5px 0 0 0; font-size: 14px;">
                      Universidad de Antioquia
                    </p>
                  </div>
                
                  <div style="padding: 30px 20px;">
                    <h2 style="color: #3d5a1e;">Semillero caracterizado</h2>
                    <p>Estimado/a administrador/a,</p>
                    <p>
                      El semillero <strong>%s</strong> (código: <strong>%s</strong>)
                      ha completado su proceso de caracterización y está listo para revisión.
                    </p>
                    <p>Por favor ingrese al sistema para revisar y aprobar la información registrada.</p>
                  </div>
                
                  <div style="background: #f0f0f0; padding: 15px 20px; text-align: center;
                              font-size: 12px; color: #777;">
                    Sistema de Semilleros — Universidad de Antioquia<br>
                    Este es un mensaje automático, por favor no responder a este correo.
                  </div>
                
                </body>
                </html>
                """.formatted(
                semillero.getNombre(),
                semillero.getCodigo()
        );
    }

    private String valorODefecto(String valor) {
        return (valor != null && !valor.isBlank()) ? valor : "No especificado";
    }
}
