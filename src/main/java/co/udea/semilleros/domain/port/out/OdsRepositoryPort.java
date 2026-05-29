package co.udea.semilleros.domain.port.out;

import java.util.Optional;

public interface OdsRepositoryPort {

    void guardarOds(
            Long   idSemillero,
            Long   idAreaOcde,
            String subAreaOcde,
            Long   idOdsPrincipal,
            String observacionesFinales
    );

    Optional<OdsDto> obtenerPorSemillero(Long idSemillero);

    record OdsDto(
            Long idAreaOcde, String nombreAreaOcde,
            String subAreaOcde,
            Long idOdsPrincipal, String nombreOdsPrincipal,
            String observacionesFinales
    ) {}
}
