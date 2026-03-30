package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.AccesoNoAutorizadoException;
import co.udea.semilleros.domain.exception.CamposObligatoriosPendientesException;
import co.udea.semilleros.domain.exception.RecursoNoEncontradoException;
import co.udea.semilleros.domain.exception.SemilleroYaExisteException;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.port.out.NotificacionEmailPort;
import co.udea.semilleros.domain.port.out.SemilleroRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GestionarSemilleroUseCase - Pruebas unitarias")
class GestionarSemilleroUseCaseImplTest {

    @Mock
    private SemilleroRepositoryPort semilleroRepositoryPort;
    @Mock
    private NotificacionEmailPort notificacionEmailPort;

    @InjectMocks
    private GestionarSemilleroUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "correoAdministrador", "admin@udea.edu.co");
    }

    // ─── crearSemilleroBorrador ────────────────────────────────────────────────

    @Test
    @DisplayName("crearSemilleroBorrador: debe crear borrador con código único cuando el coordinador no tiene semillero activo")
    void crearSemilleroBorrador_sinSemilleroExistente_creaConCodigoUnico() {
        // ARRANGE
        Long idCoordinador = 1L;
        Semillero borradorGuardado = Semillero.builder()
                .id(1L)
                .codigo("SEM-UDEA-0001")
                .estado(Semillero.EstadoSemillero.BORRADOR)
                .idCoordinador(idCoordinador)
                .build();

        when(semilleroRepositoryPort.buscarPorCoordinador(idCoordinador)).thenReturn(Optional.empty());
        when(semilleroRepositoryPort.contarPorEstado(any())).thenReturn(0L);
        when(semilleroRepositoryPort.existePorCodigo(any())).thenReturn(false);
        when(semilleroRepositoryPort.guardar(any())).thenReturn(borradorGuardado);

        // ACT
        Semillero resultado = useCase.crearSemilleroBorrador(idCoordinador);

        // ASSERT
        assertThat(resultado).isNotNull();
        assertThat(resultado.getCodigo()).isEqualTo("SEM-UDEA-0001");
        assertThat(resultado.getEstado()).isEqualTo(Semillero.EstadoSemillero.BORRADOR);
        verify(semilleroRepositoryPort).guardar(any());
    }

    @Test
    @DisplayName("crearSemilleroBorrador: debe lanzar SemilleroYaExisteException cuando el coordinador ya tiene semillero activo")
    void crearSemilleroBorrador_conSemilleroActivo_lanzaExcepcion() {
        // ARRANGE
        Long idCoordinador = 1L;
        Semillero existente = Semillero.builder()
                .id(1L)
                .estado(Semillero.EstadoSemillero.ACTIVO)
                .idCoordinador(idCoordinador)
                .build();

        when(semilleroRepositoryPort.buscarPorCoordinador(idCoordinador)).thenReturn(Optional.of(existente));

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.crearSemilleroBorrador(idCoordinador))
                .isInstanceOf(SemilleroYaExisteException.class);
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
                .correoSemillero("robotica@udea.edu.co")
                .mision("Misión del semillero")
                .vision("Visión del semillero")
                .objetivo("Objetivo del semillero")
                .idUnidadAcademica(1L)
                .idCampus(1L)
                .idAreaOcde(2L)
                .build();

        Semillero guardado = existente
                .withNombre("Semillero de Robótica")
                .withEstadoCaracterizacion("GENERAL_COMPLETADO");

        when(semilleroRepositoryPort.buscarPorId(idSemillero)).thenReturn(Optional.of(existente));
        when(semilleroRepositoryPort.existePorNombre("Semillero de Robótica")).thenReturn(false);
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
                .idCoordinador(idCoordinador)
                .build();

        Semillero finalizado = semillero
                .withEstado(Semillero.EstadoSemillero.CARACTERIZADO)
                .withEstadoCaracterizacion("COMPLETO");

        when(semilleroRepositoryPort.buscarPorId(idSemillero)).thenReturn(Optional.of(semillero));
        when(semilleroRepositoryPort.guardar(any())).thenReturn(finalizado);

        // ACT
        Semillero resultado = useCase.finalizarCaracterizacion(idSemillero, idCoordinador);

        // ASSERT
        assertThat(resultado.getEstado()).isEqualTo(Semillero.EstadoSemillero.CARACTERIZADO);
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
}
