package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.AccesoNoAutorizadoException;
import co.udea.semilleros.domain.exception.DatosAsistenciaInvalidosException;
import co.udea.semilleros.domain.exception.RecursoNoEncontradoException;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.model.asistencia.ConteoAsistencia;
import co.udea.semilleros.domain.model.asistencia.DatosSesion;
import co.udea.semilleros.domain.model.asistencia.EstadoAsistencia;
import co.udea.semilleros.domain.model.asistencia.IntegranteAsistencia;
import co.udea.semilleros.domain.model.asistencia.Sesion;
import co.udea.semilleros.domain.model.asistencia.SesionDetalle;
import co.udea.semilleros.domain.port.out.AsistenciaRepositoryPort;
import co.udea.semilleros.domain.port.out.SemilleroRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
@DisplayName("GestionarAsistenciaUseCaseImpl - Pruebas unitarias")
class GestionarAsistenciaUseCaseImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-24T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDate HOY = LocalDate.of(2026, 9, 24);
    private static final Long COORDINADOR = 5L;
    private static final Long SEMILLERO = 10L;

    @Mock
    private AsistenciaRepositoryPort asistenciaRepositoryPort;
    @Mock
    private SemilleroRepositoryPort semilleroRepositoryPort;

    private GestionarAsistenciaUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GestionarAsistenciaUseCaseImpl(asistenciaRepositoryPort, semilleroRepositoryPort, CLOCK);
    }

    private void semilleroDe(Long idCoordinador) {
        when(semilleroRepositoryPort.buscarPorId(SEMILLERO))
                .thenReturn(Optional.of(Semillero.builder().id(SEMILLERO).idCoordinador(idCoordinador).build()));
    }

    private static DatosSesion datos(LocalDate fecha, DatosSesion.Registro... registros) {
        return new DatosSesion("Club de revista", fecha, 1L, List.of(registros));
    }

    private static DatosSesion.Registro registro(long id, EstadoAsistencia estado) {
        return new DatosSesion.Registro(id, estado);
    }

    private static SesionDetalle detalle(long idSesion) {
        return new SesionDetalle(new Sesion(idSesion, SEMILLERO, 1L, "Clubes de Revista", "Club de revista", HOY,
                new ConteoAsistencia(1, 1, 0)), List.of());
    }

    @Test
    @DisplayName("registrarSesion: completa como AUSENTE a los integrantes activos que no vienen en la lista")
    void registrarSesion_completaLista() {
        semilleroDe(COORDINADOR);
        when(asistenciaRepositoryPort.idsIntegrantesActivos(SEMILLERO)).thenReturn(List.of(1L, 2L, 3L));
        when(asistenciaRepositoryPort.existeActividad(1L)).thenReturn(true);
        when(asistenciaRepositoryPort.crearSesion(eq(SEMILLERO), any())).thenReturn(99L);
        when(asistenciaRepositoryPort.obtenerSesion(99L)).thenReturn(Optional.of(detalle(99L)));

        SesionDetalle resultado = useCase.registrarSesion(COORDINADOR, SEMILLERO,
                datos(HOY, registro(1, EstadoAsistencia.PRESENTE), registro(3, EstadoAsistencia.EXCUSADO)));

        assertThat(resultado.sesion().id()).isEqualTo(99L);
        ArgumentCaptor<DatosSesion> captor = ArgumentCaptor.forClass(DatosSesion.class);
        verify(asistenciaRepositoryPort).crearSesion(eq(SEMILLERO), captor.capture());
        assertThat(captor.getValue().asistencias()).containsExactly(
                registro(1, EstadoAsistencia.PRESENTE),
                registro(3, EstadoAsistencia.EXCUSADO),
                registro(2, EstadoAsistencia.AUSENTE));
    }

    @Test
    @DisplayName("registrarSesion: sin tipo de actividad no valida el catálogo")
    void registrarSesion_sinActividad() {
        semilleroDe(COORDINADOR);
        when(asistenciaRepositoryPort.idsIntegrantesActivos(SEMILLERO)).thenReturn(List.of());
        when(asistenciaRepositoryPort.crearSesion(eq(SEMILLERO), any())).thenReturn(7L);
        when(asistenciaRepositoryPort.obtenerSesion(7L)).thenReturn(Optional.of(detalle(7L)));

        useCase.registrarSesion(COORDINADOR, SEMILLERO, new DatosSesion("Reunión", HOY, null, List.of()));

        verify(asistenciaRepositoryPort, never()).existeActividad(anyLong());
    }

    @Test
    @DisplayName("registrarSesion: rechaza fechas futuras")
    void registrarSesion_fechaFutura() {
        semilleroDe(COORDINADOR);
        when(asistenciaRepositoryPort.idsIntegrantesActivos(SEMILLERO)).thenReturn(List.of(1L));

        assertThatThrownBy(() -> useCase.registrarSesion(COORDINADOR, SEMILLERO, datos(HOY.plusDays(1))))
                .isInstanceOf(DatosAsistenciaInvalidosException.class)
                .hasMessageContaining("futura");
    }

    @Test
    @DisplayName("registrarSesion: rechaza tipos de actividad inexistentes")
    void registrarSesion_actividadInexistente() {
        semilleroDe(COORDINADOR);
        when(asistenciaRepositoryPort.idsIntegrantesActivos(SEMILLERO)).thenReturn(List.of(1L));
        when(asistenciaRepositoryPort.existeActividad(1L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.registrarSesion(COORDINADOR, SEMILLERO, datos(HOY)))
                .isInstanceOf(DatosAsistenciaInvalidosException.class);
    }

    @Test
    @DisplayName("registrarSesion: rechaza integrantes repetidos o que no son activos del semillero")
    void registrarSesion_integrantesInvalidos() {
        semilleroDe(COORDINADOR);
        when(asistenciaRepositoryPort.idsIntegrantesActivos(SEMILLERO)).thenReturn(List.of(1L));
        when(asistenciaRepositoryPort.existeActividad(1L)).thenReturn(true);

        assertThatThrownBy(() -> useCase.registrarSesion(COORDINADOR, SEMILLERO,
                datos(HOY, registro(1, EstadoAsistencia.PRESENTE), registro(1, EstadoAsistencia.AUSENTE))))
                .isInstanceOf(DatosAsistenciaInvalidosException.class).hasMessageContaining("más de una vez");
        assertThatThrownBy(() -> useCase.registrarSesion(COORDINADOR, SEMILLERO,
                datos(HOY, registro(8, EstadoAsistencia.PRESENTE))))
                .isInstanceOf(DatosAsistenciaInvalidosException.class).hasMessageContaining("no es un integrante activo");
        verify(asistenciaRepositoryPort, never()).crearSesion(any(), any());
    }

    @Test
    @DisplayName("registrarSesion: un coordinador no puede registrar en un semillero ajeno")
    void registrarSesion_semilleroAjeno() {
        semilleroDe(99L);

        assertThatThrownBy(() -> useCase.registrarSesion(COORDINADOR, SEMILLERO, datos(HOY)))
                .isInstanceOf(AccesoNoAutorizadoException.class);
    }

    @Test
    @DisplayName("registrarSesion: semillero inexistente retorna no encontrado")
    void registrarSesion_semilleroInexistente() {
        when(semilleroRepositoryPort.buscarPorId(SEMILLERO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.registrarSesion(COORDINADOR, SEMILLERO, datos(HOY)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("actualizarSesion: acepta integrantes de la lista original y activos nuevos")
    void actualizarSesion_aceptaListaOriginalYActivos() {
        when(asistenciaRepositoryPort.semilleroDeSesion(3L)).thenReturn(Optional.of(SEMILLERO));
        semilleroDe(COORDINADOR);
        when(asistenciaRepositoryPort.idsIntegrantesDeSesion(3L)).thenReturn(List.of(1L, 2L));
        when(asistenciaRepositoryPort.idsIntegrantesActivos(SEMILLERO)).thenReturn(List.of(1L, 4L));
        when(asistenciaRepositoryPort.existeActividad(1L)).thenReturn(true);
        when(asistenciaRepositoryPort.obtenerSesion(3L)).thenReturn(Optional.of(detalle(3L)));
        DatosSesion correccion = datos(HOY.minusDays(3), registro(2, EstadoAsistencia.EXCUSADO), registro(4, EstadoAsistencia.PRESENTE));

        assertThat(useCase.actualizarSesion(COORDINADOR, 3L, correccion).sesion().id()).isEqualTo(3L);
        verify(asistenciaRepositoryPort).actualizarSesion(3L, correccion);
    }

    @Test
    @DisplayName("actualizar, obtener y eliminar: validan que la sesión exista y sea del coordinador")
    void operacionesSobreSesion_validanPropiedad() {
        when(asistenciaRepositoryPort.semilleroDeSesion(3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.obtenerSesion(COORDINADOR, 3L)).isInstanceOf(RecursoNoEncontradoException.class);

        when(asistenciaRepositoryPort.semilleroDeSesion(4L)).thenReturn(Optional.of(SEMILLERO));
        semilleroDe(99L);
        assertThatThrownBy(() -> useCase.eliminarSesion(COORDINADOR, 4L)).isInstanceOf(AccesoNoAutorizadoException.class);
        verify(asistenciaRepositoryPort, never()).eliminarSesion(anyLong());
    }

    @Test
    @DisplayName("obtenerSesion y eliminarSesion: operan sobre sesiones propias")
    void obtenerYEliminar() {
        when(asistenciaRepositoryPort.semilleroDeSesion(3L)).thenReturn(Optional.of(SEMILLERO));
        semilleroDe(COORDINADOR);
        when(asistenciaRepositoryPort.obtenerSesion(3L)).thenReturn(Optional.of(detalle(3L)));

        assertThat(useCase.obtenerSesion(COORDINADOR, 3L).sesion().titulo()).isEqualTo("Club de revista");
        useCase.eliminarSesion(COORDINADOR, 3L);
        verify(asistenciaRepositoryPort).eliminarSesion(3L);
    }

    @Test
    @DisplayName("listarSesiones y asistenciaPorIntegrante: traducen el período a un rango de fechas")
    void consultas_usanRangoDelPeriodo() {
        semilleroDe(COORDINADOR);
        List<Sesion> sesiones = List.of(detalle(1L).sesion());
        List<IntegranteAsistencia> integrantes = List.of(
                new IntegranteAsistencia(1L, "Ana Pérez", "123", true, new ConteoAsistencia(3, 1, 1)));
        when(asistenciaRepositoryPort.listarSesiones(SEMILLERO, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(sesiones);
        when(asistenciaRepositoryPort.asistenciaPorIntegrante(SEMILLERO, null, null)).thenReturn(integrantes);

        assertThat(useCase.listarSesiones(COORDINADOR, SEMILLERO, "2026-2")).isEqualTo(sesiones);
        assertThat(useCase.asistenciaPorIntegrante(COORDINADOR, SEMILLERO, null)).isEqualTo(integrantes);
    }

    @Test
    @DisplayName("ConteoAsistencia: descuenta las excusas del total esperado")
    void conteoAsistencia_descuentaExcusas() {
        ConteoAsistencia conteo = new ConteoAsistencia(240, 40, 20);

        assertThat(conteo.esperadas()).isEqualTo(280);
        assertThat(conteo.porcentaje()).isEqualTo(85.7);
        assertThat(new ConteoAsistencia(0, 0, 3).porcentaje()).isNull();
        assertThat(ConteoAsistencia.VACIO.porcentaje()).isNull();
    }
}
