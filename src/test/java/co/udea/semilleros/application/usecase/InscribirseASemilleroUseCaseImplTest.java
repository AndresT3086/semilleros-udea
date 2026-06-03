package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.InscripcionDuplicadaException;
import co.udea.semilleros.domain.exception.RecursoNoEncontradoException;
import co.udea.semilleros.domain.model.Coordinador;
import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.port.out.CoordinadorRepositoryPort;
import co.udea.semilleros.domain.port.out.InscripcionRepositoryPort;
import co.udea.semilleros.domain.port.out.NotificacionEmailPort;
import co.udea.semilleros.domain.port.out.SemilleroRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InscribirseASemilleroUseCase - Pruebas unitarias")
class InscribirseASemilleroUseCaseImplTest {

    @Mock
    private InscripcionRepositoryPort inscripcionRepositoryPort;
    @Mock
    private SemilleroRepositoryPort semilleroRepositoryPort;
    @Mock
    private CoordinadorRepositoryPort coordinadorRepositoryPort;
    @Mock
    private NotificacionEmailPort notificacionEmailPort;

    @InjectMocks
    private InscribirseASemilleroUseCaseImpl useCase;

    // ─── inscribir - casos exitosos ────────────────────────────────────────────

    @Test
    @DisplayName("inscribir: debe guardar inscripción con estado PENDIENTE para correo válido")
    void inscribir_conCorreoValido_guardaConEstadoPendiente() {
        // ARRANGE
        Inscripcion request = Inscripcion.builder()
                .idSemillero(1L)
                .nombres("Juan")
                .apellidos("Pérez")
                .cedula("1040123456")
                .correo("juan.perez@udea.edu.co")
                .telefono("3001234567")
                .aceptaTerminos(true)
                .build();

        Semillero semillero = Semillero.builder()
                .id(1L)
                .nombre("Semillero IA")
                .idCoordinador(10L)
                .build();

        Coordinador coordinador = Coordinador.builder()
                .id(10L)
                .correo("coordinador@udea.edu.co")
                .build();

        Inscripcion guardada = request
                .withEstado(Inscripcion.EstadoInscripcion.PENDIENTE)
                .withNombreSemillero("Semillero IA");

        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(inscripcionRepositoryPort.existeInscripcionActivaPorCorreoYSemillero(any(), any())).thenReturn(false);
        when(inscripcionRepositoryPort.guardar(any())).thenReturn(guardada);
        when(coordinadorRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(coordinador));

        // ACT
        Inscripcion resultado = useCase.inscribir(request);

        // ASSERT
        assertThat(resultado).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(Inscripcion.EstadoInscripcion.PENDIENTE);
        assertThat(resultado.getNombreSemillero()).isEqualTo("Semillero IA");
        verify(inscripcionRepositoryPort).guardar(any());
        verify(notificacionEmailPort).notificarNuevaInscripcion(any(), any());
    }

    @Test
    @DisplayName("inscribir: debe permitir correo no institucional")
    void inscribir_conCorreoNoInstitucional_guardaConEstadoPendiente() {
        // ARRANGE
        Inscripcion request = Inscripcion.builder()
                .idSemillero(1L)
                .nombres("Juan")
                .apellidos("Pérez")
                .cedula("1040123456")
                .correo("juan.perez@gmail.com")
                .telefono("3001234567")
                .aceptaTerminos(true)
                .build();

        Semillero semillero = Semillero.builder()
                .id(1L)
                .nombre("Semillero IA")
                .idCoordinador(10L)
                .build();

        Coordinador coordinador = Coordinador.builder()
                .id(10L)
                .correo("coordinador@udea.edu.co")
                .build();

        Inscripcion guardada = request
                .withEstado(Inscripcion.EstadoInscripcion.PENDIENTE)
                .withNombreSemillero("Semillero IA");

        when(semilleroRepositoryPort.buscarPorId(1L))
                .thenReturn(Optional.of(semillero));
        when(inscripcionRepositoryPort.existeInscripcionActivaPorCorreoYSemillero(any(), any())).thenReturn(false);
        when(inscripcionRepositoryPort.guardar(any())).thenReturn(guardada);
        when(coordinadorRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(coordinador));

        // ACT
        Inscripcion resultado = useCase.inscribir(request);

        // ASSERT
        assertThat(resultado.getEstado()).isEqualTo(Inscripcion.EstadoInscripcion.PENDIENTE);
        assertThat(resultado.getCorreo()).isEqualTo("juan.perez@gmail.com");
        verify(inscripcionRepositoryPort).guardar(any());
    }

    // ─── inscribir - semillero no encontrado ───────────────────────────────────

    @Test
    @DisplayName("inscribir: debe lanzar RecursoNoEncontradoException cuando el semillero no existe")
    void inscribir_conSemilleroInexistente_lanzaExcepcion() {
        // ARRANGE
        Inscripcion request = Inscripcion.builder()
                .idSemillero(999L)
                .correo("juan.perez@udea.edu.co")
                .build();

        when(semilleroRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.inscribir(request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("999");
    }

    // ─── inscribir - inscripción duplicada ─────────────────────────────────────

    @Test
    @DisplayName("inscribir: debe lanzar InscripcionDuplicadaException cuando ya existe inscripción activa")
    void inscribir_conInscripcionDuplicada_lanzaExcepcion() {
        // ARRANGE
        Inscripcion request = Inscripcion.builder()
                .idSemillero(1L)
                .correo("juan.perez@udea.edu.co")
                .build();

        Semillero semillero = Semillero.builder().id(1L).nombre("Semillero IA").idCoordinador(10L).build();

        when(semilleroRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(semillero));
        when(inscripcionRepositoryPort.existeInscripcionActivaPorCorreoYSemillero(
                "juan.perez@udea.edu.co", 1L)).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.inscribir(request))
                .isInstanceOf(InscripcionDuplicadaException.class)
                .hasMessageContaining("juan.perez@udea.edu.co");

        verify(inscripcionRepositoryPort, never()).guardar(any());
    }
}
