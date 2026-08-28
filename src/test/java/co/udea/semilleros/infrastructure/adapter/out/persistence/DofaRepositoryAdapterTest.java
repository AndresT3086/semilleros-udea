package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.port.out.DofaRepositoryPort;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.DofaEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.DofaJpaRepository;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.SemilleroJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DofaRepositoryAdapter - Pruebas unitarias")
class DofaRepositoryAdapterTest {

    @Mock private DofaJpaRepository dofaJpaRepository;
    @Mock private SemilleroJpaRepository semilleroJpaRepository;

    private DofaRepositoryAdapter adapter() {
        return new DofaRepositoryAdapter(dofaJpaRepository, semilleroJpaRepository);
    }

    @Test
    @DisplayName("guardarDofa: debe borrar lo previo y guardar las 4 categorías referenciando el semillero")
    void guardarDofa_borraYGuardaCuatroCategorias() {
        // ARRANGE
        when(semilleroJpaRepository.getReferenceById(1L)).thenReturn(SemilleroEntity.builder().id(1L).build());

        // ACT
        adapter().guardarDofa(1L, "F", "D", "O", "A");

        // ASSERT
        verify(dofaJpaRepository).deleteBySemilleroId(1L);

        ArgumentCaptor<List<DofaEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(dofaJpaRepository).saveAll(captor.capture());

        List<DofaEntity> guardadas = captor.getValue();
        assertThat(guardadas).hasSize(4);
        assertThat(guardadas).extracting(DofaEntity::getTipo)
                .containsExactly("FORTALEZA", "DEBILIDAD", "OPORTUNIDAD", "AMENAZA");
        assertThat(guardadas).extracting(DofaEntity::getDescripcion)
                .containsExactly("F", "D", "O", "A");
    }

    @Test
    @DisplayName("obtenerPorSemillero: debe extraer cada categoría por tipo")
    void obtenerPorSemillero_conRegistros_extraePorTipo() {
        SemilleroEntity semillero = SemilleroEntity.builder().id(1L).build();
        List<DofaEntity> items = List.of(
                DofaEntity.builder().semillero(semillero).tipo("FORTALEZA").descripcion("F").build(),
                DofaEntity.builder().semillero(semillero).tipo("DEBILIDAD").descripcion("D").build(),
                DofaEntity.builder().semillero(semillero).tipo("OPORTUNIDAD").descripcion("O").build(),
                DofaEntity.builder().semillero(semillero).tipo("AMENAZA").descripcion("A").build()
        );
        when(dofaJpaRepository.findBySemilleroId(1L)).thenReturn(items);

        Optional<DofaRepositoryPort.DofaDto> resultado = adapter().obtenerPorSemillero(1L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().fortalezas()).isEqualTo("F");
        assertThat(resultado.get().debilidades()).isEqualTo("D");
        assertThat(resultado.get().oportunidades()).isEqualTo("O");
        assertThat(resultado.get().amenazas()).isEqualTo("A");
    }

    @Test
    @DisplayName("obtenerPorSemillero: debe retornar vacío cuando no hay registros")
    void obtenerPorSemillero_sinRegistros_retornaVacio() {
        when(dofaJpaRepository.findBySemilleroId(2L)).thenReturn(List.of());

        assertThat(adapter().obtenerPorSemillero(2L)).isEmpty();
    }
}
