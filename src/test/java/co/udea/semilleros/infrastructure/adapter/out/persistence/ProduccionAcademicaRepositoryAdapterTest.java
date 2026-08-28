package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.port.out.ProduccionAcademicaRepositoryPort;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.ProduccionAcademicaEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.ProduccionAcademicaJpaRepository;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.SemilleroJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProduccionAcademicaRepositoryAdapter - Pruebas unitarias")
class ProduccionAcademicaRepositoryAdapterTest {

    @Mock private ProduccionAcademicaJpaRepository produccionJpaRepository;
    @Mock private SemilleroJpaRepository semilleroJpaRepository;

    private ProduccionAcademicaRepositoryAdapter adapter() {
        return new ProduccionAcademicaRepositoryAdapter(produccionJpaRepository, semilleroJpaRepository);
    }

    @Test
    @DisplayName("guardarProduccionResumen: registro nuevo debe crear entidad referenciando el semillero")
    void guardarProduccionResumen_registroNuevo_creaEntidad() {
        // ARRANGE
        when(produccionJpaRepository.findByIdSemillero(1L)).thenReturn(Optional.empty());
        when(semilleroJpaRepository.getReferenceById(1L))
                .thenReturn(SemilleroEntity.builder().id(1L).build());

        // ACT
        adapter().guardarProduccionResumen(1L,
                true, 3, false, null, true, 2, false, null);

        // ASSERT
        ArgumentCaptor<ProduccionAcademicaEntity> captor = ArgumentCaptor.forClass(ProduccionAcademicaEntity.class);
        verify(produccionJpaRepository).save(captor.capture());

        ProduccionAcademicaEntity guardada = captor.getValue();
        assertThat(guardada.getTienenArticulos()).isTrue();
        assertThat(guardada.getCantidadArticulos()).isEqualTo(3);
        assertThat(guardada.getTienenLibros()).isFalse();
        assertThat(guardada.getCantidadLibros()).isZero();
        assertThat(guardada.getOrganizanEventos()).isTrue();
        assertThat(guardada.getParticipaEnEventos()).isFalse();
        assertThat(guardada.getCantidadParticipaciones()).isZero();
    }

    @Test
    @DisplayName("guardarProduccionResumen: registro existente debe actualizarse en vez de crear otro")
    void guardarProduccionResumen_registroExistente_actualiza() {
        // ARRANGE
        ProduccionAcademicaEntity existente = ProduccionAcademicaEntity.builder().idSemillero(1L).build();
        when(produccionJpaRepository.findByIdSemillero(1L)).thenReturn(Optional.of(existente));

        // ACT
        adapter().guardarProduccionResumen(1L,
                false, null, true, 5, false, null, true, 4);

        // ASSERT
        ArgumentCaptor<ProduccionAcademicaEntity> captor = ArgumentCaptor.forClass(ProduccionAcademicaEntity.class);
        verify(produccionJpaRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existente);
        assertThat(captor.getValue().getCantidadLibros()).isEqualTo(5);
        assertThat(captor.getValue().getCantidadParticipaciones()).isEqualTo(4);
    }

    @Test
    @DisplayName("obtenerPorSemillero: debe mapear el registro existente a DTO")
    void obtenerPorSemillero_existente_retornaDto() {
        ProduccionAcademicaEntity entity = ProduccionAcademicaEntity.builder()
                .idSemillero(1L).tienenArticulos(true).cantidadArticulos(2)
                .tienenLibros(false).cantidadLibros(0)
                .organizanEventos(true).cantidadEventos(1)
                .participaEnEventos(false).cantidadParticipaciones(0)
                .build();
        when(produccionJpaRepository.findByIdSemillero(1L)).thenReturn(Optional.of(entity));

        Optional<ProduccionAcademicaRepositoryPort.ProduccionResumenDto> resultado =
                adapter().obtenerPorSemillero(1L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().cantidadArticulos()).isEqualTo(2);
    }

    @Test
    @DisplayName("obtenerPorSemillero: debe retornar vacío cuando no hay registro")
    void obtenerPorSemillero_inexistente_retornaVacio() {
        when(produccionJpaRepository.findByIdSemillero(2L)).thenReturn(Optional.empty());

        assertThat(adapter().obtenerPorSemillero(2L)).isEmpty();
    }
}
