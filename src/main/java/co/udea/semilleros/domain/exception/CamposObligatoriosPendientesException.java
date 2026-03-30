package co.udea.semilleros.domain.exception;

import java.util.List;

public class CamposObligatoriosPendientesException extends SemillerosException {

    private static final String ERROR_CODE = "CAMPOS_OBLIGATORIOS_PENDIENTES";

    public CamposObligatoriosPendientesException(String pestana, List<String> camposFaltantes) {
        super(ERROR_CODE,
                String.format("La pestaña '%s' tiene campos obligatorios sin diligenciar: %s.",
                        pestana, String.join(", ", camposFaltantes)));
    }
}
