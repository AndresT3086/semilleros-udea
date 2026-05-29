package co.udea.semilleros.domain.port.out;

import java.util.List;
import java.util.Optional;

public interface ProduccionAcademicaRepositoryPort {

    void guardarProduccionResumen(
            Long    idSemillero,
            Boolean tienenArticulos,    Integer cantidadArticulos,
            Boolean tienenLibros,       Integer cantidadLibros,
            Boolean organizanEventos,   Integer cantidadEventos,
            Boolean participanEventos,  Integer cantidadParticipaciones
    );

    Optional<ProduccionResumenDto> obtenerPorSemillero(Long idSemillero);

    record ProduccionResumenDto(
            Boolean tienenArticulos,    Integer cantidadArticulos,
            Boolean tienenLibros,       Integer cantidadLibros,
            Boolean organizanEventos,   Integer cantidadEventos,
            Boolean participaEnEventos, Integer cantidadParticipaciones
    ) {}
}
