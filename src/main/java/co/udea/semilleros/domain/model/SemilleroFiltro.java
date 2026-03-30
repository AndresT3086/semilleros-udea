package co.udea.semilleros.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SemilleroFiltro {
    private final String palabraClave;
    private final Long idUnidadAcademica;
    private final Long idAreaOcde;
    private final Long idCampus;
    private final Integer pagina;
    private final Integer tamano;
}
