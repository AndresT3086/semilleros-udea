package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.AccesoNoAutorizadoException;
import co.udea.semilleros.domain.exception.CamposObligatoriosPendientesException;
import co.udea.semilleros.domain.exception.RecursoNoEncontradoException;
import co.udea.semilleros.domain.exception.SemilleroYaExisteException;
import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.port.in.GestionarSemilleroUseCase;
import co.udea.semilleros.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GestionarSemilleroUseCaseImpl implements GestionarSemilleroUseCase {

    private final SemilleroRepositoryPort semilleroRepositoryPort;
    private final InscripcionRepositoryPort inscripcionRepositoryPort;
    private final SemilleroIntegranteRepositoryPort semilleroIntegranteRepositoryPort;
    private final NotificacionEmailPort notificacionEmailPort;
    private final ProduccionAcademicaRepositoryPort produccionRepositoryPort;
    private final OrganizacionSemilleroRepositoryPort organizacionRepositoryPort;
    private final DofaRepositoryPort dofaRepositoryPort;
    private final OdsRepositoryPort odsRepositoryPort;
    private final ActividadesRepositoryPort actividadesRepositoryPort;
    private final RelacionamientoRepositoryPort relacionamientoRepositoryPort;

    private static final String SEMILLERO = "Semillero";

    @Value("${app.admin.correo:admin@udea.edu.co}")
    private String correoAdministrador;

    @Override
    @Transactional
    public Semillero crearSemilleroBorrador(Long idCoordinador) {

        List<Semillero> existentes = semilleroRepositoryPort.buscarPorCoordinadorYEstados(
                idCoordinador,
                List.of(Semillero.EstadoSemillero.BORRADOR)
        );

        boolean tieneBorradorSinNombre = existentes.stream()
                .anyMatch(s -> s.getNombre() == null || s.getNombre().isBlank());

        if (tieneBorradorSinNombre) {
            return existentes.stream()
                    .filter(s -> s.getNombre() == null || s.getNombre().isBlank())
                    .findFirst()
                    .get();
        }

        String codigo = generarCodigoUnico();

        Semillero borrador = Semillero.builder()
                .codigo(codigo)
                .estado(Semillero.EstadoSemillero.BORRADOR)
                .estadoCaracterizacion("GENERAL_PENDIENTE")
                .idCoordinador(idCoordinador)
                .fechaCreacion(LocalDateTime.now())
                .build();

        return semilleroRepositoryPort.guardar(borrador);
    }

    @Override
    @Transactional
    public Semillero guardarPestanaGeneral(Long idSemillero, Long idCoordinador, Semillero datos) {
        Semillero existente = semilleroRepositoryPort.buscarPorId(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException(SEMILLERO, idSemillero));

        validarPropiedadDelCoordinador(existente, idCoordinador);
        validarCamposObligatoriosGeneral(datos);

        String nombreExistente = existente.getNombre();
        String nombreNuevo = datos.getNombre();
        boolean nombreCambia = !nombreNuevo.equals(nombreExistente);

        if (nombreCambia && semilleroRepositoryPort.existePorNombre(nombreNuevo)) {
            throw new SemilleroYaExisteException("nombre", nombreNuevo);
        }

        Semillero actualizado = existente
                .withNombre(datos.getNombre())
                .withSiglas(datos.getSiglas())
                .withCorreoSemillero(datos.getCorreoSemillero())
                .withTelefono(datos.getTelefono())
                .withAnioCreacion(datos.getAnioCreacion())
                .withMision(datos.getMision())
                .withVision(datos.getVision())
                .withObjetivo(datos.getObjetivo())
                .withLineasInvestigacion(datos.getLineasInvestigacion())
                .withPalabrasClave(datos.getPalabrasClave())
                .withGrupoInvestigacion(datos.getGrupoInvestigacion())
                .withIdUnidadAcademica(datos.getIdUnidadAcademica())
                .withIdCampus(datos.getIdCampus())
                .withIdAreaOcde(datos.getIdAreaOcde())
                .withEstadoCaracterizacion("GENERAL_COMPLETADO")
                .withFechaActualizacion(LocalDateTime.now());

        return semilleroRepositoryPort.guardar(actualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Semillero> obtenerSemillerosDelCoordinador(Long idCoordinador) {
        return semilleroRepositoryPort.buscarPorCoordinador(idCoordinador);
    }

    @Override
    @Transactional(readOnly = true)
    public Semillero obtenerSemilleroDelCoordinadorPorId(Long idSemillero, Long idCoordinador) {
        Semillero semillero = semilleroRepositoryPort.buscarPorId(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException(SEMILLERO, idSemillero));

        validarPropiedadDelCoordinador(semillero, idCoordinador);
        return semillero;
    }

    private void validarPropiedadDelCoordinador(Semillero semillero, Long idCoordinador) {
        if (!semillero.getIdCoordinador().equals(idCoordinador)) {
            throw new AccesoNoAutorizadoException("semillero con id " + semillero.getId());
        }
    }

    private void validarCamposObligatoriosGeneral(Semillero datos) {
        List<String> faltantes = new ArrayList<>();

        if (datos.getNombre() == null || datos.getNombre().isBlank()) faltantes.add("nombre");
        if (datos.getCorreoSemillero() == null || datos.getCorreoSemillero().isBlank()) faltantes.add("correoSemillero");
        if (datos.getMision() == null || datos.getMision().isBlank()) faltantes.add("mision");
        if (datos.getVision() == null || datos.getVision().isBlank()) faltantes.add("vision");
        if (datos.getObjetivo() == null || datos.getObjetivo().isBlank()) faltantes.add("objetivo");
        if (datos.getIdUnidadAcademica() == null) faltantes.add("unidadAcademica");
        if (datos.getIdCampus() == null) faltantes.add("campus");
        if (datos.getIdAreaOcde() == null) faltantes.add("areaOcde");

        if (!faltantes.isEmpty()) {
            throw new CamposObligatoriosPendientesException("General", faltantes);
        }
    }

    private String generarCodigoUnico() {
        long consecutivo = semilleroRepositoryPort.contarPorEstado(Semillero.EstadoSemillero.ACTIVO)
                + semilleroRepositoryPort.contarPorEstado(Semillero.EstadoSemillero.BORRADOR)
                + semilleroRepositoryPort.contarPorEstado(Semillero.EstadoSemillero.CARACTERIZADO)
                + 1;

        String candidato = String.format("SEM-UDEA-%04d", consecutivo);

        while (semilleroRepositoryPort.existePorCodigo(candidato)) {
            consecutivo++;
            candidato = String.format("SEM-UDEA-%04d", consecutivo);
        }

        return candidato;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inscripcion> listarInscripcionesPendientes(Long idSemillero, Long idCoordinador) {
        Semillero semillero = semilleroRepositoryPort.buscarPorId(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException(SEMILLERO, idSemillero));

        validarPropiedadDelCoordinador(semillero, idCoordinador);

        return inscripcionRepositoryPort.buscarPorSemilleroYEstado(
                idSemillero, Inscripcion.EstadoInscripcion.PENDIENTE);
    }

    @Override
    @Transactional
    public Inscripcion aprobarInscripcion(Long idInscripcion, Long idCoordinador) {
        Inscripcion inscripcion = inscripcionRepositoryPort.buscarPorId(idInscripcion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción", idInscripcion));

        Semillero semillero = semilleroRepositoryPort.buscarPorId(inscripcion.getIdSemillero())
                .orElseThrow(() -> new RecursoNoEncontradoException(SEMILLERO, inscripcion.getIdSemillero()));

        validarPropiedadDelCoordinador(semillero, idCoordinador);

        Inscripcion aprobada = inscripcion
                .withEstado(Inscripcion.EstadoInscripcion.APROBADO)
                .withFechaActualizacion(LocalDateTime.now());

        Inscripcion guardada = inscripcionRepositoryPort.guardar(aprobada);

        semilleroIntegranteRepositoryPort.registrarIntegrante(
                inscripcion.getIdSemillero(),
                inscripcion.getNombres(),
                inscripcion.getApellidos(),
                inscripcion.getCedula(),
                inscripcion.getCorreo(),
                "ESTUDIANTE"
        );

        return guardada;
    }

    @Override
    @Transactional
    public Inscripcion rechazarInscripcion(Long idInscripcion, Long idCoordinador) {
        Inscripcion inscripcion = inscripcionRepositoryPort.buscarPorId(idInscripcion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción", idInscripcion));

        Semillero semillero = semilleroRepositoryPort.buscarPorId(inscripcion.getIdSemillero())
                .orElseThrow(() -> new RecursoNoEncontradoException(SEMILLERO, inscripcion.getIdSemillero()));

        validarPropiedadDelCoordinador(semillero, idCoordinador);

        Inscripcion rechazada = inscripcion
                .withEstado(Inscripcion.EstadoInscripcion.RECHAZADO)
                .withFechaActualizacion(LocalDateTime.now());

        return inscripcionRepositoryPort.guardar(rechazada);
    }

    @Override
    @Transactional
    public Semillero guardarPestanaProduccion(
            Long idSemillero, Long idCoordinador,
            Boolean tienenArticulos,    Integer cantidadArticulos,
            Boolean tienenLibros,       Integer cantidadLibros,
            Boolean organizanEventos,   Integer cantidadEventos,
            Boolean participanEventos,  Integer cantidadParticipaciones) {

        Semillero semillero = obtenerYValidar(idSemillero, idCoordinador);

        produccionRepositoryPort.guardarProduccionResumen(
                idSemillero,
                tienenArticulos,   cantidadArticulos,
                tienenLibros,      cantidadLibros,
                organizanEventos,  cantidadEventos,
                participanEventos, cantidadParticipaciones
        );

        return actualizarEstadoPestana(semillero, "PRODUCCION");
    }

    @Override
    @Transactional
    public Semillero guardarPestanaOrganizacion(Long idSemillero, Long idCoordinador,
                                                List<Long> idsRecursos, List<Long> idsFuentes) {

        Semillero semillero = obtenerYValidar(idSemillero, idCoordinador);

        if (idsRecursos == null || idsRecursos.isEmpty()) {
            throw new CamposObligatoriosPendientesException("Organización",
                    List.of("recursos (seleccione al menos uno o Ninguno)"));
        }
        if (idsFuentes == null || idsFuentes.isEmpty()) {
            throw new CamposObligatoriosPendientesException("Organización",
                    List.of("fuentesFinanciacion (seleccione al menos una o Sin financiación)"));
        }

        organizacionRepositoryPort.guardarRecursos(idSemillero, idsRecursos);
        organizacionRepositoryPort.guardarFuentesFinanciacion(idSemillero, idsFuentes);

        return actualizarEstadoPestana(semillero, "ORGANIZACION");
    }

    @Override
    @Transactional
    public Semillero guardarPestanaRelacionamiento(
            Long idSemillero,
            Long idCoordinador,
            Boolean adscritoGrupo,
            String grupoInvestigacion,
            String relacionGrupo,
            String centroInvestigaciones,
            String relacionCentro,
            String departamento,
            String relacionDepartamento,
            String facultad,
            String relacionFacultad) {

        Semillero semillero = obtenerYValidar(idSemillero, idCoordinador);

        relacionamientoRepositoryPort.guardarRelacionamiento(
                idSemillero,
                adscritoGrupo,
                grupoInvestigacion,
                relacionGrupo,
                centroInvestigaciones,
                relacionCentro,
                departamento,
                relacionDepartamento,
                facultad,
                relacionFacultad
        );
        return actualizarEstadoPestana(semillero, "RELACIONAMIENTO");
    }

    @Override
    @Transactional
    public Semillero guardarPestanaActividades(Long idSemillero, Long idCoordinador,
                                               List<ActividadesRepositoryPort.ActividadDto> actividades) {

        Semillero semillero = obtenerYValidar(idSemillero, idCoordinador);

        if (actividades == null || actividades.isEmpty()) {
            throw new CamposObligatoriosPendientesException("Actividades",
                    List.of("actividades (debe indicar al menos una actividad)"));
        }

        actividadesRepositoryPort.actualizarActividades(idSemillero, actividades);

        return actualizarEstadoPestana(semillero, "ACTIVIDADES");
    }

    @Override
    @Transactional
    public Semillero guardarPestanaDofa(Long idSemillero, Long idCoordinador,
                                        String fortalezas, String debilidades,
                                        String oportunidades, String amenazas) {

        Semillero semillero = obtenerYValidar(idSemillero, idCoordinador);

        List<String> faltantes = new ArrayList<>();
        if (fortalezas   == null || fortalezas.isBlank())   faltantes.add("fortalezas");
        if (debilidades  == null || debilidades.isBlank())  faltantes.add("debilidades");
        if (oportunidades == null || oportunidades.isBlank()) faltantes.add("oportunidades");
        if (amenazas     == null || amenazas.isBlank())     faltantes.add("amenazas");

        if (!faltantes.isEmpty()) {
            throw new CamposObligatoriosPendientesException("DOFA", faltantes);
        }

        dofaRepositoryPort.guardarDofa(idSemillero, fortalezas, debilidades, oportunidades, amenazas);

        return actualizarEstadoPestana(semillero, "DOFA");
    }

    @Override
    @Transactional
    public Semillero guardarPestanaOds(
            Long idSemillero, Long idCoordinador,
            Long idAreaOcde, String subAreaOcde,
            Long idOdsPrincipal, String observacionesFinales) {

        Semillero semillero = obtenerYValidar(idSemillero, idCoordinador);

        if (idAreaOcde == null) {
            throw new CamposObligatoriosPendientesException("ODS", List.of("areaOcde"));
        }
        if (idOdsPrincipal == null) {
            throw new CamposObligatoriosPendientesException("ODS", List.of("odsPrincipal"));
        }

        odsRepositoryPort.guardarOds(idSemillero, idAreaOcde, subAreaOcde,
                idOdsPrincipal, observacionesFinales);

        return actualizarEstadoPestana(semillero, "ODS");
    }

    private Semillero actualizarEstadoPestana(Semillero semillero, String pestana) {
        String estadoActual = semillero.getEstadoCaracterizacion();
        String marcador     = pestana + "_COMPLETADO";

        String nuevoEstado;
        if (estadoActual == null || estadoActual.isBlank()) {
            nuevoEstado = marcador;
        } else if (estadoActual.contains(marcador)) {
            nuevoEstado = estadoActual; // ya estaba marcada, no duplicar
        } else {
            nuevoEstado = estadoActual + "," + marcador;
        }

        Semillero actualizado = semillero
                .withEstadoCaracterizacion(nuevoEstado)
                .withFechaActualizacion(LocalDateTime.now());

        return semilleroRepositoryPort.guardar(actualizado);
    }

    @Override
    @Transactional
    public Semillero finalizarCaracterizacion(Long idSemillero, Long idCoordinador) {
        Semillero semillero = obtenerYValidar(idSemillero, idCoordinador);

        String estado = semillero.getEstadoCaracterizacion();

        List<String> pestanasObligatorias = List.of(
                "GENERAL", "PRODUCCION", "ORGANIZACION",
                "ACTIVIDADES", "DOFA", "ODS"
        );

        List<String> faltantes = pestanasObligatorias.stream()
                .filter(p -> estado == null || !estado.contains(p + "_COMPLETADO"))
                .map(p -> "Pestaña " + p + " incompleta")
                .toList();

        if (!faltantes.isEmpty()) {
            throw new CamposObligatoriosPendientesException("Finalización", faltantes);
        }

        Semillero finalizado = semillero
                .withEstado(Semillero.EstadoSemillero.ACTIVO)
                .withEstadoCaracterizacion("COMPLETO")
                .withFechaActualizacion(LocalDateTime.now());

        Semillero guardado = semilleroRepositoryPort.guardar(finalizado);
        notificacionEmailPort.notificarFinalizacionCaracterizacion(guardado, correoAdministrador);
        return guardado;
    }

    private Semillero obtenerYValidar(Long idSemillero, Long idCoordinador) {
        Semillero semillero = semilleroRepositoryPort.buscarPorId(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", idSemillero));
        validarPropiedadDelCoordinador(semillero, idCoordinador);
        return semillero;
    }
}
