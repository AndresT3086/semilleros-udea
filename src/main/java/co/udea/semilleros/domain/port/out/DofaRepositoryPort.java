package co.udea.semilleros.domain.port.out;

import java.util.Optional;

public interface DofaRepositoryPort {

    void guardarDofa(Long idSemillero,
                     String fortalezas,
                     String debilidades,
                     String oportunidades,
                     String amenazas);

    Optional<DofaDto> obtenerPorSemillero(Long idSemillero);

    record DofaDto(
            String fortalezas,
            String debilidades,
            String oportunidades,
            String amenazas
    ) {}
}
