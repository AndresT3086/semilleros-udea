package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroIntegranteEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.SemilleroIntegranteJpaRepository;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.SemilleroJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SemilleroIntegranteRepositoryAdapter - Pruebas unitarias")
class SemilleroIntegranteRepositoryAdapterTest {

    @Mock private SemilleroIntegranteJpaRepository integranteJpaRepository;
    @Mock private SemilleroJpaRepository semilleroJpaRepository;

    private SemilleroIntegranteRepositoryAdapter adapter() {
        return new SemilleroIntegranteRepositoryAdapter(integranteJpaRepository, semilleroJpaRepository);
    }

    @Test
    @DisplayName("registrarIntegrante: debe construir el integrante activo con referencia al semillero y guardarlo")
    void registrarIntegrante_construyeYGuarda() {
        // ARRANGE
        SemilleroEntity semilleroRef = SemilleroEntity.builder().id(1L).build();
        when(semilleroJpaRepository.getReferenceById(1L)).thenReturn(semilleroRef);

        // ACT
        adapter().registrarIntegrante(1L, "Juan", "Pérez", "123", "juan@udea.edu.co", "MASCULINO",
                "ESTUDIANTE_INVESTIGADOR");

        // ASSERT
        ArgumentCaptor<SemilleroIntegranteEntity> captor = ArgumentCaptor.forClass(SemilleroIntegranteEntity.class);
        verify(integranteJpaRepository).save(captor.capture());

        SemilleroIntegranteEntity guardado = captor.getValue();
        assertThat(guardado.getSemillero()).isEqualTo(semilleroRef);
        assertThat(guardado.getNombres()).isEqualTo("Juan");
        assertThat(guardado.getApellidos()).isEqualTo("Pérez");
        assertThat(guardado.getCedula()).isEqualTo("123");
        assertThat(guardado.getCorreo()).isEqualTo("juan@udea.edu.co");
        assertThat(guardado.getSexo()).isEqualTo("MASCULINO");
        assertThat(guardado.getTipoVinculacion()).isEqualTo("ESTUDIANTE_INVESTIGADOR");
        assertThat(guardado.getActivo()).isTrue();
        assertThat(guardado.getFechaIngreso()).isNotNull();
    }
}
