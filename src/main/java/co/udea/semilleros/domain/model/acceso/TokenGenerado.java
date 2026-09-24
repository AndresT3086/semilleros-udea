package co.udea.semilleros.domain.model.acceso;

/**
 * Token aleatorio: {@code valor} viaja solo en el correo y {@code hash} es lo único que se guarda.
 */
public record TokenGenerado(String valor, String hash) {

    @Override
    public String toString() {
        return "TokenGenerado[hash=" + hash + "]";
    }
}
