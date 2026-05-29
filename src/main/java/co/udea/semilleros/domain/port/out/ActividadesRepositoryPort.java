package co.udea.semilleros.domain.port.out;

import java.util.List;

public interface ActividadesRepositoryPort {

    void actualizarActividades(Long idSemillero, List<ActividadDto> actividades);

    List<ActividadDetalleDto> obtenerTodasConEstadoPorSemillero(Long idSemillero);

    record ActividadDto(Long idActividad, Boolean realiza) {}

    record ActividadDetalleDto(
            Long idActividad, String nombre, String categoria, Boolean realiza) {}


}
