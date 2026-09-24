package co.udea.semilleros.domain.port.out;

import co.udea.semilleros.domain.model.acceso.TokenGenerado;

/**
 * Genera tokens aleatorios para enlaces de un solo uso y calcula el hash que se guarda.
 */
public interface TokenSeguroPort {

    TokenGenerado generar();

    String hash(String valor);
}
