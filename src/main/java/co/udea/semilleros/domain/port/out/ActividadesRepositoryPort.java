package co.udea.semilleros.domain.port.out;

import java.util.List;

public interface ActividadesRepositoryPort {

    void actualizarActividades(Long idSemillero, List<ActividadDto> actividades);

    record ActividadDto(Long idActividad, Boolean realiza) {}
}
