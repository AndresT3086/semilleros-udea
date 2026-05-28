package co.udea.semilleros.domain.port.out;

import java.util.List;

public interface ProduccionAcademicaRepositoryPort {

    void guardarProduccionResumen(
            Long    idSemillero,
            Boolean tienenArticulos,    Integer cantidadArticulos,
            Boolean tienenLibros,       Integer cantidadLibros,
            Boolean organizanEventos,   Integer cantidadEventos,
            Boolean participanEventos,  Integer cantidadParticipaciones
    );
}
