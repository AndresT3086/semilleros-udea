package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.model.SemilleroFiltro;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.*;
import co.udea.semilleros.infrastructure.adapter.out.persistence.mapper.SemilleroEntityMapperImpl;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SemilleroRepositoryAdapter - Pruebas unitarias")
class SemilleroRepositoryAdapterTest {

    @Mock private SemilleroJpaRepository semilleroJpaRepository;
    @Mock private UnidadAcademicaJpaRepository unidadAcademicaJpaRepository;
    @Mock private CampusJpaRepository campusJpaRepository;
    @Mock private AreaOcdeJpaRepository areaOcdeJpaRepository;
    @Mock private UsuarioJpaRepository usuarioJpaRepository;

    private final SemilleroEntityMapperImpl semilleroEntityMapper = new SemilleroEntityMapperImpl();

    private SemilleroRepositoryAdapter adapter() {
        return new SemilleroRepositoryAdapter(
                semilleroJpaRepository, semilleroEntityMapper,
                unidadAcademicaJpaRepository, campusJpaRepository,
                areaOcdeJpaRepository, usuarioJpaRepository);
    }

    private SemilleroEntity entidadBase() {
        return SemilleroEntity.builder()
                .id(1L)
                .codigo("SEM-UDEA-0001")
                .nombre("Semillero IA")
                .estado(SemilleroEntity.EstadoSemilleroJpa.ACTIVO)
                .build();
    }

    // ─── buscarActivos ──────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarActivos: debe paginar, normalizar palabra clave y sumar conteos")
    void buscarActivos_conFiltroYConteos_retornaPageResultConConteos() {
        // ARRANGE
        SemilleroFiltro filtro = SemilleroFiltro.builder()
                .pagina(0).tamano(15).palabraClave("  robotica  ")
                .idUnidadAcademica(1L).idCampus(2L).idAreaOcde(3L)
                .build();

        Page<SemilleroEntity> page = new PageImpl<>(List.of(entidadBase()), PageRequest.of(0, 15), 1);

        when(semilleroJpaRepository.buscarActivos(eq(1L), eq(2L), eq(3L), eq("robotica"), any()))
                .thenReturn(page);
        when(semilleroJpaRepository.contarSemilleristas(1L)).thenReturn(5);
        when(semilleroJpaRepository.contarActividadesCientificas(1L)).thenReturn(null);

        // ACT
        PageResult<Semillero> resultado = adapter().buscarActivos(filtro);

        // ASSERT
        assertThat(resultado.getContenido()).hasSize(1);
        assertThat(resultado.getContenido().get(0).getTotalSemilleristas()).isEqualTo(5);
        assertThat(resultado.getContenido().get(0).getTotalActividadesCientificas()).isZero();
        assertThat(resultado.getTotalElementos()).isEqualTo(1L);
    }

    @Test
    @DisplayName("buscarActivos: debe usar página/tamaño por defecto y palabra clave nula cuando vienen en blanco")
    void buscarActivos_sinPaginaNiPalabraClave_usaDefaults() {
        // ARRANGE
        SemilleroFiltro filtro = SemilleroFiltro.builder().palabraClave("   ").build();
        Page<SemilleroEntity> page = new PageImpl<>(List.of());

        when(semilleroJpaRepository.buscarActivos(isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(page);

        // ACT
        PageResult<Semillero> resultado = adapter().buscarActivos(filtro);

        // ASSERT
        assertThat(resultado.getContenido()).isEmpty();
        verify(semilleroJpaRepository).buscarActivos(isNull(), isNull(), isNull(), isNull(), any());
    }

    // ─── buscarPorId / buscarPorCodigo ─────────────────────────────────────

    @Test
    @DisplayName("buscarPorId: debe retornar semillero con conteos cuando existe")
    void buscarPorId_existente_retornaConConteos() {
        // ARRANGE
        when(semilleroJpaRepository.findById(1L)).thenReturn(Optional.of(entidadBase()));
        when(semilleroJpaRepository.contarSemilleristas(1L)).thenReturn(2);
        when(semilleroJpaRepository.contarActividadesCientificas(1L)).thenReturn(3);

        // ACT
        Optional<Semillero> resultado = adapter().buscarPorId(1L);

        // ASSERT
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getTotalSemilleristas()).isEqualTo(2);
    }

    @Test
    @DisplayName("buscarPorId: debe retornar vacío cuando no existe")
    void buscarPorId_inexistente_retornaVacio() {
        when(semilleroJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(adapter().buscarPorId(999L)).isEmpty();
    }

    @Test
    @DisplayName("buscarPorCodigo: debe mapear a dominio cuando existe")
    void buscarPorCodigo_existente_retornaSemillero() {
        when(semilleroJpaRepository.findByCodigo("SEM-UDEA-0001")).thenReturn(Optional.of(entidadBase()));

        Optional<Semillero> resultado = adapter().buscarPorCodigo("SEM-UDEA-0001");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getCodigo()).isEqualTo("SEM-UDEA-0001");
    }

    // ─── buscarPorCoordinador / buscarPorCoordinadorYEstados ───────────────

    @Test
    @DisplayName("buscarPorCoordinador: debe mapear la lista de entidades a dominio")
    void buscarPorCoordinador_retornaLista() {
        when(semilleroJpaRepository.findByCoordinadorId(10L)).thenReturn(List.of(entidadBase()));

        List<Semillero> resultado = adapter().buscarPorCoordinador(10L);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("buscarPorCoordinadorYEstados: debe traducir estados de dominio a JPA")
    void buscarPorCoordinadorYEstados_traduceEstados() {
        when(semilleroJpaRepository.findByCoordinadorIdAndEstadoIn(
                eq(10L), eq(List.of(SemilleroEntity.EstadoSemilleroJpa.BORRADOR))))
                .thenReturn(List.of(entidadBase()));

        List<Semillero> resultado = adapter().buscarPorCoordinadorYEstados(
                10L, List.of(Semillero.EstadoSemillero.BORRADOR));

        assertThat(resultado).hasSize(1);
    }

    // ─── guardar ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("guardar: semillero nuevo debe resolver relaciones existentes y guardar")
    void guardar_semilleroNuevo_resuelveRelacionesYGuarda() {
        // ARRANGE
        Semillero nuevo = Semillero.builder()
                .codigo("SEM-UDEA-0002")
                .nombre("Semillero Bio")
                .estado(Semillero.EstadoSemillero.BORRADOR)
                .idUnidadAcademica(1L).idCampus(2L).idAreaOcde(3L).idCoordinador(4L)
                .build();

        when(unidadAcademicaJpaRepository.findById(1L))
                .thenReturn(Optional.of(UnidadAcademicaEntity.builder().id(1L).nombre("Ingeniería").build()));
        when(campusJpaRepository.findById(2L))
                .thenReturn(Optional.of(CampusEntity.builder().id(2L).nombre("Central").build()));
        when(areaOcdeJpaRepository.findById(3L))
                .thenReturn(Optional.of(AreaOcdeEntity.builder().id(3L).nombre("Biología").build()));
        when(usuarioJpaRepository.findById(4L))
                .thenReturn(Optional.of(UsuarioEntity.builder().id(4L).correo("c@udea.edu.co").build()));
        when(semilleroJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        Semillero resultado = adapter().guardar(nuevo);

        // ASSERT
        assertThat(resultado.getNombre()).isEqualTo("Semillero Bio");
        assertThat(resultado.getIdUnidadAcademica()).isEqualTo(1L);
        assertThat(resultado.getIdCampus()).isEqualTo(2L);
        assertThat(resultado.getIdAreaOcde()).isEqualTo(3L);
        assertThat(resultado.getIdCoordinador()).isEqualTo(4L);
    }

    @Test
    @DisplayName("guardar: semillero existente preserva fechaCreacion y con relaciones inexistentes no las setea")
    void guardar_semilleroExistente_preservaFechaCreacionYRelacionesVacias() {
        // ARRANGE
        Semillero existenteDatos = Semillero.builder()
                .id(1L).codigo("SEM-UDEA-0001").nombre("Semillero IA")
                .estado(Semillero.EstadoSemillero.ACTIVO)
                .idUnidadAcademica(99L)
                .build();

        SemilleroEntity previa = entidadBase();

        when(semilleroJpaRepository.findById(1L)).thenReturn(Optional.of(previa));
        when(unidadAcademicaJpaRepository.findById(99L)).thenReturn(Optional.empty());
        when(semilleroJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        Semillero resultado = adapter().guardar(existenteDatos);

        // ASSERT
        assertThat(resultado.getIdUnidadAcademica()).isNull();
        verify(semilleroJpaRepository).findById(1L);
    }

    // ─── existePorNombre / existePorCodigo / contarPorEstado ───────────────

    @Test
    @DisplayName("existePorNombre y existePorCodigo delegan al repositorio JPA")
    void existePorNombreYCodigo_delegan() {
        when(semilleroJpaRepository.existsByNombre("X")).thenReturn(true);
        when(semilleroJpaRepository.existsByCodigo("Y")).thenReturn(false);

        assertThat(adapter().existePorNombre("X")).isTrue();
        assertThat(adapter().existePorCodigo("Y")).isFalse();
    }

    @Test
    @DisplayName("contarPorEstado: traduce el estado de dominio a JPA y delega")
    void contarPorEstado_traduceYDelega() {
        when(semilleroJpaRepository.countByEstado(SemilleroEntity.EstadoSemilleroJpa.ACTIVO)).thenReturn(7L);

        long total = adapter().contarPorEstado(Semillero.EstadoSemillero.ACTIVO);

        assertThat(total).isEqualTo(7L);
    }
}
