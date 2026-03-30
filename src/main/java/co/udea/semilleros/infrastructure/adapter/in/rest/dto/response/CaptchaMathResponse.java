package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CaptchaMathResponse {
    private int operando1;
    private int operando2;
    private String operacion;
    private String pregunta;
}
