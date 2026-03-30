package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InscripcionResponse {
    private Long id;
    private Long idSemillero;
    private String nombreSemillero;
    private String nombres;
    private String apellidos;
    private String correo;
    private String estado;
    private LocalDateTime fechaInscripcion;
}
