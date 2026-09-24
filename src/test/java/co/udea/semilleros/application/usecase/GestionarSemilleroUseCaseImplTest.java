package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.AccesoNoAutorizadoException;
import co.udea.semilleros.domain.exception.CamposObligatoriosPendientesException;
import co.udea.semilleros.domain.exception.RecursoNoEncontradoException;
import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.port.out.*;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.PestanaActividadesResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.PestanaDofaResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.PestanaGeneralResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.PestanaOdsResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.PestanaOrganizacionResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.PestanaProduccionResponse;
import co.udea.semilleros.infrastructure.adapter.in.rest.dto.response.PestanaRelacionamientoResponse;
import co.udea.semilleros.infrastructure.config.InputSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GestionarSemilleroUseCase - Pruebas unitarias")
class GestionarSemilleroUseCaseImplTest {

    @Mock
    private SemilleroRepositoryPort semilleroRepositoryPort;
    @Mock
    private InscripcionRepositoryPort inscripcionRepositoryPort;
    @Mock
    private SemilleroIntegranteRepositoryPort semilleroIntegranteRepositoryPort;
    @Mock
    private NotificacionEmailPort notificacionEmailPort;
    @Mock
    private ProduccionAcademicaRepositoryPort produccionRepositoryPort;
    @Mock
    private OrganizacionSemilleroRepositoryPort organizacionRepositoryPort;
    @Mock
    private DofaRepositoryPort dofaRepositoryPort;
    @Mock
    private OdsRepositoryPort odsRepositoryPort;
    @Mock
    private ActividadesRepositoryPort actividadesRepositoryPort;
    @Mock
    private RelacionamientoRepositoryPort relacionamientoRepositoryPort;
    @Mock
    private InputSanitizer inputSanitizer;
    @Mock
    private FiltrosRepositoryPort filtrosRepositoryPort;

    @InjectMocks
    private GestionarSemilleroUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "correoAdministrador", "admin@udea.edu.co");
    }


    // ─── guardarPestanaGeneral ─────────────────────────────────────────────────

    @Test
    @DisplayName("guardarPestanaGeneral: debe guardar cuando todos los campos obligatorios están completos")
    void guardarPestanaGeneral_conDatosCompletos_guardaExitosamente() {
        // ARRANGE
        Long idSemillero = 1L;
        Long idCoordinador = 1L;

        Semillero existente = Semillero.builder()
                .id(idSemillero)
                .codigo("SEM-UDEA-0001")
                .nombre("Nombre viejo")
                .estado(Semillero.EstadoSemillero.BORRADOR)
                .idCoordinador(idCoordinador)
                .build();

        Semillero datosNuevos = Semillero.builder()
                .nombre("Semillero de Robótica")
                .siglas("SEROBOT")
                .correoSemillero("robotica@udea.edu.co")
                .telefono("3001234567")
                .anioCreacion(2024)
                .mision("Misión del semillero")
                .vision("Visión del semillero")
                .objetivo("Objetivo del semillero")
                .lineasInvestigacion("Robótica educativa")
                .palabrasClave("robótica, educación")
                .grupoInvestigacion("Grupo de Robótica")
                .idUnidadAcademica(1L)
                .idCampus(1L)
                .idAreaOcde(2L)
                .build();

        Semillero guardado = existente
                .withNombre("Semillero de Robótica")
                .withEstadoCaracterizacion("GENERAL_COMPLETADO");

        when(semilleroRepositoryPort.buscarPorId(idSemillero)).thenReturn(Optional.of(existente));
        when(semilleroRepositoryPort.existePorNombre("Semillero de Robótica")).thenReturn(false);
        when(inputSanitizer.sanitizarCampoCorto("Semillero de Robótica")).thenReturn("Semillero de Robótica");
        when(inputSanitizer.sanitizar("Misión del semillero")).thenReturn("Misión del semillero");
        when(inputSanitizer.sanitizar("Visión del semillero")).thenReturn("Visión del semillero");
        when(inputSanitizer.sanitizar("Objetivo del semillero")).thenReturn("Objetivo del semillero");
        when(semilleroRepositoryPort.guardar(any())).thenReturn(guardado);

        // ACT
        Semillero resultado = useCase.guardarPestanaGeneral(idSemillero, idCoordinador, datosNuevos);

        // ASSERT
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNombre()).isEqualTo("Semillero de Robótica");
        assertThat(resultado.getEstadoCaracterizacion()).isEqualTo("GENERAL_COMPLETADO");
    }

    @Test
    @DisplayName("guardarPestanaGeneral: debe lanzar CamposObligatoriosPendientesException cuando faltan campos")
    void guardarPestanaGeneral_conCamposFaltantes_lanzaExcepcion() {
        // ARRANGE
        Long idSemillero = 1L;
        Long idCoordinador = 1L;

        Semillero existente = Semillero.builder()
                .id(idSemillero)
                .estado(Semillero.EstadoSemillero.BORRADOR)
                .idCoordinador(idCoordinador)
                .build();

        Semillero datosIncompletos = Semillero.builder()
                .nombre("") // nombre vacío
                .build();

        when(semilleroRepositoryPort.buscarPorId(idSemillero)).thenReturn(Optional.of(existente));

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.guardarPestanaGeneral(idSemillero, idCoordinador, datosIncompletos))
                .isInstanceOf(CamposObligatoriosPendientesException.class)
                .hasMessageContaining("General");
    }

    @Test
    @DisplayName("guardarPestanaGeneral: debe lanzar AccesoNoAutorizadoException cuando el coordinador no es dueño")
    void guardarPestanaGeneral_coordinadorNoPropietario_lanzaExcepcion() {
        // ARRANGE
        Long idSemillero = 1L;
        Long idCoordinadorPropietario = 1L;
        Long idOtroCoordinador = 99L;

        Semillero existente = Semillero.builder()
                .id(idSemillero)
                .idCoordinador(idCoordinadorPropietario)
                .build();

        when(semilleroRepositoryPort.buscarPorId(idSemillero)).thenReturn(Optional.of(existente));

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.guardarPestanaGeneral(idSemillero, idOtroCoordinador, Semillero.builder().build()))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    // ─── finalizarCaracterizacion ──────────────────────────────────────────────

    @Test
    @DisplayName("finalizarCaracterizacion: debe cambiar estado a CARACTERIZADO y notificar al administrador")
    void finalizarCaracterizacion_conDatosValidos_cambiaPEstadoYNotifica() {
        // ARRANGE
        Long idSemillero = 1L;
        Long idCoordinador = 1L;

        Semillero semillero = Semillero.builder()
                .id(idSemillero)
                .nombre("Semillero IA")
                .codigo("SEM-UDEA-0001")
                .estado(Semillero.EstadoSemillero.ACTIVO)
                .estadoCaracterizacion("GENERAL_COMPLETADO,PRODUCCION_COMPLETADO,ORGANIZACION_COMPLETADO,ACTIVIDADES_COMPLETADO,DOFA_COMPLETADO,ODS_COMPLETADO")
                .idCoordinador(idCoordinador)
                .build();

        Semillero finalizado = semillero
                .withEstado(Semillero.EstadoSemillero.ACTIVO)
                .withEstadoCaracterizacion("COMPLETO");

        when(semilleroRepositoryPort.buscarPorId(idSemillero)).thenReturn(Optional.of(semillero));
        when(semilleroRepositoryPort.guardar(any())).thenReturn(finalizado);

        // ACT
        Semillero resultado = useCase.finalizarCaracterizacion(idSemillero, idCoordinador);

        // ASSERT
        assertThat(resultado.getEstado()).isEqualTo(Semillero.EstadoSemillero.ACTIVO);
        assertThat(resultado.getEstadoCaracterizacion()).isEqualTo("COMPLETO");
        verify(notificacionEmailPort).notificarFinalizacionCaracterizacion(any(), any());
    }

    @Test
    @DisplayName("finalizarCaracterizacion: debe lanzar RecursoNoEncontradoException cuando el semillero no existe")
    void finalizarCaracterizacion_conSemilleroInexistente_lanzaExcepcion() {
        // ARRANGE
        Long idInexistente = 999L;
        when(semilleroRepositoryPort.buscarPorId(idInexistente)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.finalizarCaracterizacion(idInexistente, 1L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("999");
    }

    // ─── gestión de inscripciones ─────────────────────────────────────────────

    @Test
    @DisplayName("listarInscripcionesPendientes: debe retornar solicitudes del semillero del coordinador")
    void listarInscripcionesPendientes_conSemilleroPropio_retornaPendientes() {
        // ARRANGE
        Long idSemillero = 1L;
        Long idCoordinador = 1L;

        Semillero semillero = Semillero.builder()
                .id(idSemillero)
                .idCoordinador(idCoordinador)
                .build();
        Inscripcion inscripcion = crearInscripcion(idSemillero)
                .withEstado(Inscripcion.EstadoInscripcion.PENDIENTE);

        when(semilleroRepositoryPort.buscarPorId(idSemillero)).thenReturn(Optional.of(semillero));
        when(inscripcionRepositoryPort.buscarPorSemilleroYEstado(
                idSemillero, Inscripcion.EstadoInscripcion.PENDIENTE))
                .thenReturn(List.of(inscripcion));

        // ACT
        List<Inscripcion> resultado = useCase.listarInscripcionesPendientes(idSemillero, idCoordinador);

        // ASSERT
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo(Inscripcion.EstadoInscripcion.PENDIENTE);
    }

    @Test
    @DisplayName("aprobarInscripcion: debe aprobar y registrar integrante")
    void aprobarInscripcion_conSolicitudValida_apruebaYRegistraIntegrante() {
        // ARRANGE
        Long idInscripcion = 10L;
        Long idSemillero = 1L;
        Long idCoordinador = 1L;

        Inscripcion pendiente = crearInscripcion(idSemillero)
                .withId(idInscripcion)
                .withEstado(Inscripcion.EstadoInscripcion.PENDIENTE);
        Semillero semillero = Semillero.builder()
                .id(idSemillero)
                .idCoordinador(idCoordinador)
                .build();
        Inscripcion aprobada = pendiente.withEstado(Inscripcion.EstadoInscripcion.APROBADO);

        when(inscripcionRepositoryPort.buscarPorId(idInscripcion)).thenReturn(Optional.of(pendiente));
        when(semilleroRepositoryPort.buscarPorId(idSemillero)).thenReturn(Optional.of(semillero));
        when(inscripcionRepositoryPort.guardar(any())).thenReturn(aprobada);

        // ACT
        Inscripcion resultado = useCase.aprobarInscripcion(idInscripcion, idCoordinador);

        // ASSERT
        assertThat(resultado.getEstado()).isEqualTo(Inscripcion.EstadoInscripcion.APROBADO);
        verify(inscripcionRepositoryPort).guardar(any());
        verify(semilleroIntegranteRepositoryPort).registrarIntegrante(
                eq(idSemillero), eq("Ana"), eq("Pérez"), eq("12345678"),
                eq("ana@example.com"), eq("FEMENINO"), eq("ESTUDIANTE_INVESTIGADOR"));
    }

    @Test
    @DisplayName("rechazarInscripcion: debe marcar la solicitud como rechazada")
    void rechazarInscripcion_conSolicitudValida_rechazaSolicitud() {
        // ARRANGE
        Long idInscripcion = 10L;
        Long idSemillero = 1L;
        Long idCoordinador = 1L;

        Inscripcion pendiente = crearInscripcion(idSemillero)
                .withId(idInscripcion)
                .withEstado(Inscripcion.EstadoInscripcion.PENDIENTE);
        Semillero semillero = Semillero.builder()
                .id(idSemillero)
                .idCoordinador(idCoordinador)
                .build();
        Inscripcion rechazada = pendiente.withEstado(Inscripcion.EstadoInscripcion.RECHAZADO);

        when(inscripcionRepositoryPort.buscarPorId(idInscripcion)).thenReturn(Optional.of(pendiente));
        when(semilleroRepositoryPort.buscarPorId(idSemillero)).thenReturn(Optional.of(semillero));
        when(inscripcionRepositoryPort.guardar(any())).thenReturn(rechazada);

        // ACT
        Inscripcion resultado = useCase.rechazarInscripcion(idInscripcion, idCoordinador);

        // ASSERT
        assertThat(resultado.getEstado()).isEqualTo(Inscripcion.EstadoInscripcion.RECHAZADO);
        verify(inscripcionRepositoryPort).guardar(any());
    }

    private Inscripcion crearInscripcion(Long idSemillero) {
        return Inscripcion.builder()
                .idSemillero(idSemillero)
                .nombreSemillero("Semillero IA")
                .nombres("Ana")
                .apellidos("Pérez")
                .cedula("12345678")
                .correo("ana@example.com")
                .telefono("3001234567")
                .sexo("FEMENINO")
                .programa("Ingeniería de Sistemas")
                .semestre("6")
                .motivacion("Quiero participar en actividades de investigación aplicada.")
                .aceptaTerminos(true)
                .build();
    }

    @Test
    @DisplayName("aprobarInscripcion: debe lanzar RecursoNoEncontradoException cuando la inscripción no existe")
    void aprobarInscripcion_inexistente_lanzaExcepcion() {
        // ARRANGE
        when(inscripcionRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.aprobarInscripcion(999L, 5L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("finalizarCaracterizacion: debe lanzar CamposObligatoriosPendientesException cuando faltan pestañas")
    void finalizarCaracterizacion_conPestanasIncompletas_lanzaExcepcion() {
        // ARRANGE
        Semillero semillero = Semillero.builder()
                .id(1L)
                .idCoordinador(1L)
                .estadoCaracterizacion("GENERAL_COMPLETADO,PRODUCCION_COMPLETADO")
                .build();

        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.finalizarCaracterizacion(1L, 1L))
                .isInstanceOf(CamposObligatoriosPendientesException.class)
                .hasMessageContaining("Finalización");
    }

    // ─── crearSemilleroBorrador ────────────────────────────────────────────────

    @Test
    @DisplayName("crearSemilleroBorrador: debe crear un nuevo borrador con código único cuando no hay borradores previos")
    void crearSemilleroBorrador_sinBorradoresPrevios_creaNuevo() {
        // ARRANGE
        Long idCoordinador = 5L;
        Semillero nuevo = Semillero.builder()
                .id(10L)
                .codigo("SEM-UDEA-0001")
                .estado(Semillero.EstadoSemillero.BORRADOR)
                .idCoordinador(idCoordinador)
                .build();

        when(semilleroRepositoryPort.buscarPorCoordinadorYEstados(idCoordinador,
                List.of(Semillero.EstadoSemillero.BORRADOR))).thenReturn(List.of());
        when(semilleroRepositoryPort.contarPorEstado(any())).thenReturn(0L);
        when(semilleroRepositoryPort.existePorCodigo(any())).thenReturn(false);
        when(semilleroRepositoryPort.guardar(any())).thenReturn(nuevo);

        // ACT
        Semillero resultado = useCase.crearSemilleroBorrador(idCoordinador);

        // ASSERT
        assertThat(resultado.getCodigo()).isEqualTo("SEM-UDEA-0001");
        verify(semilleroRepositoryPort).guardar(any());
    }

    @Test
    @DisplayName("crearSemilleroBorrador: debe reutilizar el borrador existente sin nombre en vez de crear uno nuevo")
    void crearSemilleroBorrador_conBorradorSinNombre_reutilizaExistente() {
        // ARRANGE
        Long idCoordinador = 5L;
        Semillero borradorSinNombre = Semillero.builder()
                .id(20L)
                .codigo("SEM-UDEA-0002")
                .nombre(null)
                .estado(Semillero.EstadoSemillero.BORRADOR)
                .idCoordinador(idCoordinador)
                .build();

        when(semilleroRepositoryPort.buscarPorCoordinadorYEstados(idCoordinador,
                List.of(Semillero.EstadoSemillero.BORRADOR))).thenReturn(List.of(borradorSinNombre));

        // ACT
        Semillero resultado = useCase.crearSemilleroBorrador(idCoordinador);

        // ASSERT
        assertThat(resultado.getId()).isEqualTo(20L);
        verify(semilleroRepositoryPort, never()).guardar(any());
    }

    // ─── obtenerSemillerosDelCoordinador / obtenerSemilleroDelCoordinadorPorId ─

    @Test
    @DisplayName("obtenerSemillerosDelCoordinador: debe retornar la lista de semilleros del coordinador")
    void obtenerSemillerosDelCoordinador_retornaLista() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorCoordinador(5L)).thenReturn(List.of(semillero));

        // ACT
        List<Semillero> resultado = useCase.obtenerSemillerosDelCoordinador(5L);

        // ASSERT
        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("obtenerSemilleroDelCoordinadorPorId: debe retornar el semillero cuando pertenece al coordinador")
    void obtenerSemilleroDelCoordinadorPorId_propietario_retornaSemillero() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));

        // ACT
        Semillero resultado = useCase.obtenerSemilleroDelCoordinadorPorId(1L, 5L);

        // ASSERT
        assertThat(resultado.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("obtenerSemilleroDelCoordinadorPorId: debe lanzar RecursoNoEncontradoException cuando no existe")
    void obtenerSemilleroDelCoordinadorPorId_inexistente_lanzaExcepcion() {
        // ARRANGE
        when(semilleroRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.obtenerSemilleroDelCoordinadorPorId(999L, 5L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("obtenerSemilleroDelCoordinadorPorId: debe lanzar AccesoNoAutorizadoException cuando no es el dueño")
    void obtenerSemilleroDelCoordinadorPorId_noPropietario_lanzaExcepcion() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.obtenerSemilleroDelCoordinadorPorId(1L, 99L))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    // ─── guardarPestanaProduccion ──────────────────────────────────────────────

    @Test
    @DisplayName("guardarPestanaProduccion: debe guardar el resumen y marcar la pestaña completada")
    void guardarPestanaProduccion_conDatosValidos_guardaYMarcaCompletada() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).estadoCaracterizacion("GENERAL_COMPLETADO").build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(semilleroRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        Semillero resultado = useCase.guardarPestanaProduccion(
                1L, 5L, true, 3, false, 0, true, 2, false, 0);

        // ASSERT
        assertThat(resultado.getEstadoCaracterizacion()).contains("PRODUCCION_COMPLETADO");
        verify(produccionRepositoryPort).guardarProduccionResumen(
                1L, true, 3, false, 0, true, 2, false, 0);
    }

    // ─── guardarPestanaOrganizacion ────────────────────────────────────────────

    @Test
    @DisplayName("guardarPestanaOrganizacion: debe guardar recursos y fuentes cuando ambos están presentes")
    void guardarPestanaOrganizacion_conDatosValidos_guardaYMarcaCompletada() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).estadoCaracterizacion("").build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(semilleroRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        Semillero resultado = useCase.guardarPestanaOrganizacion(1L, 5L, List.of(1L, 2L), List.of(3L));

        // ASSERT
        assertThat(resultado.getEstadoCaracterizacion()).isEqualTo("ORGANIZACION_COMPLETADO");
        verify(organizacionRepositoryPort).guardarRecursos(1L, List.of(1L, 2L));
        verify(organizacionRepositoryPort).guardarFuentesFinanciacion(1L, List.of(3L));
    }

    @Test
    @DisplayName("guardarPestanaOrganizacion: debe lanzar excepción cuando no hay recursos seleccionados")
    void guardarPestanaOrganizacion_sinRecursos_lanzaExcepcion() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.guardarPestanaOrganizacion(1L, 5L, List.of(), List.of(3L)))
                .isInstanceOf(CamposObligatoriosPendientesException.class);
    }

    @Test
    @DisplayName("guardarPestanaOrganizacion: debe lanzar excepción cuando no hay fuentes de financiación")
    void guardarPestanaOrganizacion_sinFuentes_lanzaExcepcion() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.guardarPestanaOrganizacion(1L, 5L, List.of(1L), null))
                .isInstanceOf(CamposObligatoriosPendientesException.class);
    }

    // ─── guardarPestanaRelacionamiento ─────────────────────────────────────────

    @Test
    @DisplayName("guardarPestanaRelacionamiento: debe guardar y marcar la pestaña completada")
    void guardarPestanaRelacionamiento_conDatosValidos_guardaYMarcaCompletada() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).estadoCaracterizacion("").build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(semilleroRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        Semillero resultado = useCase.guardarPestanaRelacionamiento(
                1L, 5L, true, "Grupo X", "Aliado", null, null, null, null, null, null);

        // ASSERT
        assertThat(resultado.getEstadoCaracterizacion()).isEqualTo("RELACIONAMIENTO_COMPLETADO");
        verify(relacionamientoRepositoryPort).guardarRelacionamiento(
                1L, true, "Grupo X", "Aliado", null, null, null, null, null, null);
    }

    // ─── guardarPestanaActividades ─────────────────────────────────────────────

    @Test
    @DisplayName("guardarPestanaActividades: debe guardar y marcar la pestaña completada")
    void guardarPestanaActividades_conDatosValidos_guardaYMarcaCompletada() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).estadoCaracterizacion("").build();
        List<ActividadesRepositoryPort.ActividadDto> actividades =
                List.of(new ActividadesRepositoryPort.ActividadDto(1L, true));

        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(semilleroRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        Semillero resultado = useCase.guardarPestanaActividades(1L, 5L, actividades);

        // ASSERT
        assertThat(resultado.getEstadoCaracterizacion()).isEqualTo("ACTIVIDADES_COMPLETADO");
        verify(actividadesRepositoryPort).actualizarActividades(1L, actividades);
    }

    @Test
    @DisplayName("guardarPestanaActividades: debe lanzar excepción cuando la lista de actividades está vacía")
    void guardarPestanaActividades_listaVacia_lanzaExcepcion() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.guardarPestanaActividades(1L, 5L, List.of()))
                .isInstanceOf(CamposObligatoriosPendientesException.class);
    }

    // ─── guardarPestanaDofa ─────────────────────────────────────────────────────

    @Test
    @DisplayName("guardarPestanaDofa: debe guardar y marcar la pestaña completada")
    void guardarPestanaDofa_conDatosValidos_guardaYMarcaCompletada() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).estadoCaracterizacion("").build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(semilleroRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        Semillero resultado = useCase.guardarPestanaDofa(1L, 5L, "F", "D", "O", "A");

        // ASSERT
        assertThat(resultado.getEstadoCaracterizacion()).isEqualTo("DOFA_COMPLETADO");
        verify(dofaRepositoryPort).guardarDofa(1L, "F", "D", "O", "A");
    }

    @Test
    @DisplayName("guardarPestanaDofa: debe lanzar excepción cuando faltan campos")
    void guardarPestanaDofa_conCamposFaltantes_lanzaExcepcion() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.guardarPestanaDofa(1L, 5L, "F", null, "", "A"))
                .isInstanceOf(CamposObligatoriosPendientesException.class)
                .hasMessageContaining("DOFA");
    }

    @Test
    @DisplayName("guardarPestanaDofa: no debe duplicar el marcador cuando la pestaña ya estaba completada")
    void guardarPestanaDofa_conPestanaYaCompletada_noDuplicaMarcador() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L)
                .estadoCaracterizacion("DOFA_COMPLETADO").build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(semilleroRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        Semillero resultado = useCase.guardarPestanaDofa(1L, 5L, "F", "D", "O", "A");

        // ASSERT
        assertThat(resultado.getEstadoCaracterizacion()).isEqualTo("DOFA_COMPLETADO");
    }

    // ─── guardarPestanaOds ──────────────────────────────────────────────────────

    @Test
    @DisplayName("guardarPestanaOds: debe guardar y marcar la pestaña completada")
    void guardarPestanaOds_conDatosValidos_guardaYMarcaCompletada() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).estadoCaracterizacion("").build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(semilleroRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        Semillero resultado = useCase.guardarPestanaOds(1L, 5L, 2L, "Sub", 4L, "Obs");

        // ASSERT
        assertThat(resultado.getEstadoCaracterizacion()).isEqualTo("ODS_COMPLETADO");
        verify(odsRepositoryPort).guardarOds(1L, 2L, "Sub", 4L, "Obs");
    }

    @Test
    @DisplayName("guardarPestanaOds: debe lanzar excepción cuando falta el área OCDE")
    void guardarPestanaOds_sinAreaOcde_lanzaExcepcion() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.guardarPestanaOds(1L, 5L, null, "Sub", 4L, "Obs"))
                .isInstanceOf(CamposObligatoriosPendientesException.class);
    }

    @Test
    @DisplayName("guardarPestanaOds: debe lanzar excepción cuando falta el ODS principal")
    void guardarPestanaOds_sinOdsPrincipal_lanzaExcepcion() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.guardarPestanaOds(1L, 5L, 2L, "Sub", null, "Obs"))
                .isInstanceOf(CamposObligatoriosPendientesException.class);
    }

    // ─── obtenerPestana* ────────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerPestanaGeneral: debe retornar los datos generales del semillero")
    void obtenerPestanaGeneral_retornaDatos() {
        // ARRANGE
        Semillero semillero = Semillero.builder()
                .id(1L).idCoordinador(5L).codigo("SEM-UDEA-0001").nombre("Semillero IA").build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));

        // ACT
        PestanaGeneralResponse resultado = useCase.obtenerPestanaGeneral(1L, 5L);

        // ASSERT
        assertThat(resultado.getNombre()).isEqualTo("Semillero IA");
    }

    @Test
    @DisplayName("obtenerPestanaProduccion: debe retornar los datos guardados cuando existen")
    void obtenerPestanaProduccion_conDatos_retornaDatosGuardados() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(produccionRepositoryPort.obtenerPorSemillero(1L)).thenReturn(Optional.of(
                new ProduccionAcademicaRepositoryPort.ProduccionResumenDto(
                        true, 3, false, 0, true, 2, false, 0)));

        // ACT
        PestanaProduccionResponse resultado = useCase.obtenerPestanaProduccion(1L, 5L);

        // ASSERT
        assertThat(resultado.getCantidadArticulos()).isEqualTo(3);
    }

    @Test
    @DisplayName("obtenerPestanaProduccion: debe retornar valores por defecto cuando no hay datos guardados")
    void obtenerPestanaProduccion_sinDatos_retornaValoresPorDefecto() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(produccionRepositoryPort.obtenerPorSemillero(1L)).thenReturn(Optional.empty());

        // ACT
        PestanaProduccionResponse resultado = useCase.obtenerPestanaProduccion(1L, 5L);

        // ASSERT
        assertThat(resultado.getTienenArticulos()).isFalse();
        assertThat(resultado.getCantidadArticulos()).isZero();
    }

    @Test
    @DisplayName("obtenerPestanaOrganizacion: debe retornar recursos y fuentes seleccionados junto con el catálogo completo")
    void obtenerPestanaOrganizacion_retornaSeleccionadosYCatalogo() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(organizacionRepositoryPort.obtenerIdsRecursosPorSemillero(1L)).thenReturn(List.of(1L));
        when(organizacionRepositoryPort.obtenerIdsFuentesPorSemillero(1L)).thenReturn(List.of(3L));
        when(filtrosRepositoryPort.listarTodosLosRecursos()).thenReturn(List.of(
                new FiltrosRepositoryPort.RecursoDto(1L, "Recurso A"),
                new FiltrosRepositoryPort.RecursoDto(2L, "Recurso B")));
        when(filtrosRepositoryPort.listarTodasLasFuentes()).thenReturn(List.of(
                new FiltrosRepositoryPort.FuenteFinanciacionDto(3L, "Fuente A")));

        // ACT
        PestanaOrganizacionResponse resultado = useCase.obtenerPestanaOrganizacion(1L, 5L);

        // ASSERT
        assertThat(resultado.getRecursosSeleccionados()).hasSize(1);
        assertThat(resultado.getTodosLosRecursos()).hasSize(2);
        assertThat(resultado.getFuentesSeleccionadas()).hasSize(1);
    }

    @Test
    @DisplayName("obtenerPestanaRelacionamiento: debe retornar los datos guardados cuando existen")
    void obtenerPestanaRelacionamiento_conDatos_retornaDatosGuardados() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(relacionamientoRepositoryPort.obtenerPorSemillero(1L)).thenReturn(Optional.of(
                new RelacionamientoRepositoryPort.RelacionamientoDto(
                        true, "Grupo X", "Aliado", null, null, null, null, null, null)));

        // ACT
        PestanaRelacionamientoResponse resultado = useCase.obtenerPestanaRelacionamiento(1L, 5L);

        // ASSERT
        assertThat(resultado.getGrupoInvestigacion()).isEqualTo("Grupo X");
    }

    @Test
    @DisplayName("obtenerPestanaRelacionamiento: debe retornar valores por defecto cuando no hay datos guardados")
    void obtenerPestanaRelacionamiento_sinDatos_retornaValoresPorDefecto() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(relacionamientoRepositoryPort.obtenerPorSemillero(1L)).thenReturn(Optional.empty());

        // ACT
        PestanaRelacionamientoResponse resultado = useCase.obtenerPestanaRelacionamiento(1L, 5L);

        // ASSERT
        assertThat(resultado.getAdscritoGrupo()).isFalse();
    }

    @Test
    @DisplayName("obtenerPestanaActividades: debe retornar el listado de actividades con su estado")
    void obtenerPestanaActividades_retornaListado() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(actividadesRepositoryPort.obtenerTodasConEstadoPorSemillero(1L)).thenReturn(List.of(
                new ActividadesRepositoryPort.ActividadDetalleDto(1L, "Ponencias", "Divulgación", true)));

        // ACT
        PestanaActividadesResponse resultado = useCase.obtenerPestanaActividades(1L, 5L);

        // ASSERT
        assertThat(resultado.getActividades()).hasSize(1);
        assertThat(resultado.getActividades().get(0).getRealiza()).isTrue();
    }

    @Test
    @DisplayName("obtenerPestanaDofa: debe retornar los datos guardados cuando existen")
    void obtenerPestanaDofa_conDatos_retornaDatosGuardados() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(dofaRepositoryPort.obtenerPorSemillero(1L)).thenReturn(Optional.of(
                new DofaRepositoryPort.DofaDto("F", "D", "O", "A")));

        // ACT
        PestanaDofaResponse resultado = useCase.obtenerPestanaDofa(1L, 5L);

        // ASSERT
        assertThat(resultado.getFortalezas()).isEqualTo("F");
    }

    @Test
    @DisplayName("obtenerPestanaDofa: debe retornar respuesta vacía cuando no hay datos guardados")
    void obtenerPestanaDofa_sinDatos_retornaVacio() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(dofaRepositoryPort.obtenerPorSemillero(1L)).thenReturn(Optional.empty());

        // ACT
        PestanaDofaResponse resultado = useCase.obtenerPestanaDofa(1L, 5L);

        // ASSERT
        assertThat(resultado.getFortalezas()).isNull();
    }

    @Test
    @DisplayName("obtenerPestanaOds: debe retornar los datos guardados cuando existen")
    void obtenerPestanaOds_conDatos_retornaDatosGuardados() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(odsRepositoryPort.obtenerPorSemillero(1L)).thenReturn(Optional.of(
                new OdsRepositoryPort.OdsDto(2L, "Ciencias", "Sub", 4L, "ODS 4", "Obs")));

        // ACT
        PestanaOdsResponse resultado = useCase.obtenerPestanaOds(1L, 5L);

        // ASSERT
        assertThat(resultado.getNombreOdsPrincipal()).isEqualTo("ODS 4");
    }

    @Test
    @DisplayName("obtenerPestanaOds: debe retornar respuesta vacía cuando no hay datos guardados")
    void obtenerPestanaOds_sinDatos_retornaVacio() {
        // ARRANGE
        Semillero semillero = Semillero.builder().id(1L).idCoordinador(5L).build();
        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(odsRepositoryPort.obtenerPorSemillero(1L)).thenReturn(Optional.empty());

        // ACT
        PestanaOdsResponse resultado = useCase.obtenerPestanaOds(1L, 5L);

        // ASSERT
        assertThat(resultado.getIdAreaOcde()).isNull();
    }
}
