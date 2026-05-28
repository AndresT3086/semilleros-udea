package co.udea.semilleros.domain.port.out;

public interface OdsRepositoryPort {

    void guardarOds(
            Long   idSemillero,
            Long   idAreaOcde,
            String subAreaOcde,
            Long   idOdsPrincipal,
            String observacionesFinales
    );}
