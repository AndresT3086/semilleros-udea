package co.udea.semilleros.domain.port.in;

import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.port.out.ActividadesRepositoryPort;
import co.udea.semilleros.domain.port.out.ProduccionAcademicaRepositoryPort;

import java.util.List;

public interface GestionarSemilleroUseCase {

    Semillero crearSemilleroBorrador(Long idCoordinador);

    Semillero guardarPestanaGeneral(Long idSemillero, Long idCoordinador, Semillero datos);

    List<Semillero> obtenerSemillerosDelCoordinador(Long idCoordinador);

    Semillero obtenerSemilleroDelCoordinadorPorId(Long idSemillero, Long idCoordinador);

    Semillero finalizarCaracterizacion(Long idSemillero, Long idCoordinador);

    List<Inscripcion> listarInscripcionesPendientes(Long idSemillero, Long idCoordinador);

    Inscripcion aprobarInscripcion(Long idInscripcion, Long idCoordinador);

    Inscripcion rechazarInscripcion(Long idInscripcion, Long idCoordinador);

    Semillero guardarPestanaOrganizacion(Long idSemillero, Long idCoordinador,
                                         List<Long> idsRecursos, List<Long> idsFuentes);

    Semillero guardarPestanaActividades(Long idSemillero, Long idCoordinador,
                                        List<ActividadesRepositoryPort.ActividadDto> actividades);

    Semillero guardarPestanaDofa(Long idSemillero, Long idCoordinador,
                                 String fortalezas, String debilidades,
                                 String oportunidades, String amenazas);

    Semillero guardarPestanaProduccion(
            Long    idSemillero,
            Long    idCoordinador,
            Boolean tienenArticulos,    Integer cantidadArticulos,
            Boolean tienenLibros,       Integer cantidadLibros,
            Boolean organizanEventos,   Integer cantidadEventos,
            Boolean participanEventos,  Integer cantidadParticipaciones
    );

    Semillero guardarPestanaRelacionamiento(
            Long    idSemillero,
            Long    idCoordinador,
            Boolean adscritoGrupo,
            String  grupoInvestigacion,
            String  relacionGrupo,
            String  centroInvestigaciones,
            String  relacionCentro,
            String  departamento,
            String  relacionDepartamento,
            String  facultad,
            String  relacionFacultad
    );

    Semillero guardarPestanaOds(
            Long   idSemillero,
            Long   idCoordinador,
            Long   idAreaOcde,
            String subAreaOcde,
            Long   idOdsPrincipal,
            String observacionesFinales
    );

}
