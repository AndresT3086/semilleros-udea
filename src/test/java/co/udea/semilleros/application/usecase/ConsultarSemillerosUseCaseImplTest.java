package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.exception.RecursoNoEncontradoException;
import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.model.SemilleroFiltro;
import co.udea.semilleros.domain.port.out.SemilleroRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarSemillerosUseCase - Pruebas unitarias")
class ConsultarSemillerosUseCaseImplTest {

    @Mock
    private SemilleroRepositoryPort semilleroRepositoryPort;

    @InjectMocks
    private ConsultarSemillerosUseCaseImpl useCase;

    // ─── listarSemillerosActivos ───────────────────────────────────────────────

    @Test
    @DisplayName("listarSemillerosActivos: debe retornar página de semilleros cuando existen registros")
    void listarSemillerosActivos_conRegistros_retornaPageResult() {
        // ARRANGE
        SemilleroFiltro filtro = SemilleroFiltro.builder().pagina(0).tamano(15).build();

        Semillero semillero = Semillero.builder()
                .id(1L)
                .nombre("Semillero IA")
                .estado(Semillero.EstadoSemillero.ACTIVO)
                .build();

        PageResult<Semillero> paginaEsperada = PageResult.<Semillero>builder()
                .contenido(List.of(semillero))
                .paginaActual(0)
                .tamano(15)
                .totalElementos(1L)
                .totalPaginas(1)
                .esUltimaPagina(true)
                .esPrimeraPagina(true)
                .build();

        when(semilleroRepositoryPort.buscarActivos(any())).thenReturn(paginaEsperada);

        // ACT
        PageResult<Semillero> resultado = useCase.listarSemillerosActivos(filtro);

        // ASSERT
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContenido()).hasSize(1);
        assertThat(resultado.getTotalElementos()).isEqualTo(1L);
        assertThat(resultado.getContenido().get(0).getNombre()).isEqualTo("Semillero IA");
        verify(semilleroRepositoryPort).buscarActivos(filtro);
    }

    @Test
    @DisplayName("listarSemillerosActivos: debe retornar página vacía cuando no hay registros")
    void listarSemillerosActivos_sinRegistros_retornaPaginaVacia() {
        // ARRANGE
        SemilleroFiltro filtro = SemilleroFiltro.builder().pagina(0).tamano(15).build();

        PageResult<Semillero> paginaVacia = PageResult.<Semillero>builder()
                .contenido(List.of())
                .paginaActual(0)
                .tamano(15)
                .totalElementos(0L)
                .totalPaginas(0)
                .esUltimaPagina(true)
                .esPrimeraPagina(true)
                .build();

        when(semilleroRepositoryPort.buscarActivos(any())).thenReturn(paginaVacia);

        // ACT
        PageResult<Semillero> resultado = useCase.listarSemillerosActivos(filtro);

        // ASSERT
        assertThat(resultado.getContenido()).isEmpty();
        assertThat(resultado.getTotalElementos()).isZero();
    }

    // ─── obtenerDetalleSemillero ───────────────────────────────────────────────

    @Test
    @DisplayName("obtenerDetalleSemillero: debe retornar semillero cuando existe el ID")
    void obtenerDetalleSemillero_conIdExistente_retornaSemillero() {
        // ARRANGE
        Long idSemillero = 1L;
        Semillero semillero = Semillero.builder()
                .id(idSemillero)
                .nombre("Semillero Biotecnología")
                .estado(Semillero.EstadoSemillero.ACTIVO)
                .build();

        when(semilleroRepositoryPort.buscarPorId(idSemillero)).thenReturn(Optional.of(semillero));

        // ACT
        Semillero resultado = useCase.obtenerDetalleSemillero(idSemillero);

        // ASSERT
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(idSemillero);
        assertThat(resultado.getNombre()).isEqualTo("Semillero Biotecnología");
    }

    @Test
    @DisplayName("obtenerDetalleSemillero: debe lanzar RecursoNoEncontradoException cuando el ID no existe")
    void obtenerDetalleSemillero_conIdInexistente_lanzaExcepcion() {
        // ARRANGE
        Long idInexistente = 999L;
        when(semilleroRepositoryPort.buscarPorId(idInexistente)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> useCase.obtenerDetalleSemillero(idInexistente))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("999");
    }
}
