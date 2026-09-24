package co.udea.semilleros.domain.model.acceso;

public enum EstadoSolicitud {
    /** Formulario enviado; falta que la persona confirme su correo. El admin no la ve. */
    PENDIENTE_VERIFICACION,
    /** Correo confirmado; espera la decisión de un administrador. */
    PENDIENTE,
    APROBADA,
    RECHAZADA
}
