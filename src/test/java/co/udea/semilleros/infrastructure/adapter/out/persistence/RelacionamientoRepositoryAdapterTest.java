package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.port.out.RelacionamientoRepositoryPort;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroRelacionamientoEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.SemilleroJpaRepository;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.SemilleroRelacionamientoJpaRepository;
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
@DisplayName("RelacionamientoRepositoryAdapter - Pruebas unitarias")
class RelacionamientoRepositoryAdapterTest {

    @Mock private SemilleroRelacionamientoJpaRepository relacionamientoJpaRepository;
    @Mock private SemilleroJpaRepository semilleroJpaRepository;

    private RelacionamientoRepositoryAdapter adapter() {
        return new RelacionamientoRepositoryAdapter(relacionamientoJpaRepository, semilleroJpaRepository);
    }

    @Test
    @DisplayName("guardarRelacionamiento: registro nuevo debe crear entidad referenciando el semillero")
    void guardarRelacionamiento_registroNuevo_creaEntidad() {
        // ARRANGE
        when(relacionamientoJpaRepository.findByIdSemillero(1L)).thenReturn(Optional.empty());
        when(semilleroJpaRepository.getReferenceById(1L)).thenReturn(SemilleroEntity.builder().id(1L).build());

        // ACT
        adapter().guardarRelacionamiento(1L, true, "Grupo A", "Relación A",
                "Centro A", "Relación Centro", "Depto", "Relación Depto", "Facultad", "Relación Facultad");

        // ASSERT
        ArgumentCaptor<SemilleroRelacionamientoEntity> captor =
                ArgumentCaptor.forClass(SemilleroRelacionamientoEntity.class);
        verify(relacionamientoJpaRepository).save(captor.capture());

        SemilleroRelacionamientoEntity guardado = captor.getValue();
        assertThat(guardado.getAdscritoGrupo()).isTrue();
        assertThat(guardado.getGrupoInvestigacion()).isEqualTo("Grupo A");
        assertThat(guardado.getFacultad()).isEqualTo("Facultad");
    }

    @Test
    @DisplayName("guardarRelacionamiento: registro existente debe actualizarse")
    void guardarRelacionamiento_registroExistente_actualiza() {
        SemilleroRelacionamientoEntity existente = SemilleroRelacionamientoEntity.builder().idSemillero(1L).build();
        when(relacionamientoJpaRepository.findByIdSemillero(1L)).thenReturn(Optional.of(existente));

        adapter().guardarRelacionamiento(1L, false, null, null, null, null, null, null, null, null);

        ArgumentCaptor<SemilleroRelacionamientoEntity> captor =
                ArgumentCaptor.forClass(SemilleroRelacionamientoEntity.class);
        verify(relacionamientoJpaRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existente);
        assertThat(captor.getValue().getAdscritoGrupo()).isFalse();
    }

    @Test
    @DisplayName("obtenerPorSemillero: debe mapear el registro existente a DTO")
    void obtenerPorSemillero_existente_retornaDto() {
        SemilleroRelacionamientoEntity entity = SemilleroRelacionamientoEntity.builder()
                .idSemillero(1L).adscritoGrupo(true).grupoInvestigacion("Grupo A").build();
        when(relacionamientoJpaRepository.findByIdSemillero(1L)).thenReturn(Optional.of(entity));

        Optional<RelacionamientoRepositoryPort.RelacionamientoDto> resultado = adapter().obtenerPorSemillero(1L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().grupoInvestigacion()).isEqualTo("Grupo A");
    }

    @Test
    @DisplayName("obtenerPorSemillero: debe retornar vacío cuando no hay registro")
    void obtenerPorSemillero_inexistente_retornaVacio() {
        when(relacionamientoJpaRepository.findByIdSemillero(2L)).thenReturn(Optional.empty());

        assertThat(adapter().obtenerPorSemillero(2L)).isEmpty();
    }
}
