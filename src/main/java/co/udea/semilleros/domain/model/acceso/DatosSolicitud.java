package co.udea.semilleros.domain.model.acceso;

/**
 * Formulario público de solicitud de acceso como coordinador.
 *
 * @param sitioWeb campo trampa oculto en el formulario: una persona lo deja vacío, un bot suele llenarlo
 * @param ip       dirección de origen, para auditoría
 */
public record DatosSolicitud(
        String nombres,
        String apellidos,
        String cedula,
        String correo,
        Long idUnidadAcademica,
        String justificacion,
        String sitioWeb,
        int respuestaMath,
        int operando1,
        int operando2,
        String ip
) {
}
