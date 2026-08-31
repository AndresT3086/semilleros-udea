package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.InscripcionEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.mapper.InscripcionEntityMapperImpl;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.InscripcionJpaRepository;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.SemilleroJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InscripcionRepositoryAdapter - Pruebas unitarias")
class InscripcionRepositoryAdapterTest {

    @Mock private InscripcionJpaRepository inscripcionJpaRepository;
    @Mock private SemilleroJpaRepository semilleroJpaRepository;

    private final InscripcionEntityMapperImpl mapper = new InscripcionEntityMapperImpl();

    private InscripcionRepositoryAdapter adapter() {
        return new InscripcionRepositoryAdapter(inscripcionJpaRepository, mapper, semilleroJpaRepository);
    }

    @Test
    @DisplayName("guardar: debe asociar el semillero por referencia y remapear a dominio")
    void guardar_asociaSemilleroYRemapea() {
        // ARRANGE
        Inscripcion nueva = Inscripcion.builder()
                .idSemillero(1L).correo("juan@udea.edu.co")
                .estado(Inscripcion.EstadoInscripcion.PENDIENTE)
                .build();

        SemilleroEntity semilleroRef = SemilleroEntity.builder().id(1L).nombre("Semillero IA").build();
        when(semilleroJpaRepository.getReferenceById(1L)).thenReturn(semilleroRef);

        InscripcionEntity guardada = InscripcionEntity.builder()
                .id(9L).semillero(semilleroRef).correo("juan@udea.edu.co")
                .estado(InscripcionEntity.EstadoInscripcionJpa.PENDIENTE)
                .build();
        when(inscripcionJpaRepository.save(any())).thenReturn(guardada);

        // ACT
        Inscripcion resultado = adapter().guardar(nueva);

        // ASSERT
        assertThat(resultado.getId()).isEqualTo(9L);
        assertThat(resultado.getIdSemillero()).isEqualTo(1L);
        assertThat(resultado.getNombreSemillero()).isEqualTo("Semillero IA");
    }

    @Test
    @DisplayName("existeInscripcionActivaPorCorreoYSemillero: delega al repositorio JPA")
    void existeInscripcionActiva_delega() {
        when(inscripcionJpaRepository.existeInscripcionActivaPorCorreoYSemillero("juan@udea.edu.co", 1L))
                .thenReturn(true);

        assertThat(adapter().existeInscripcionActivaPorCorreoYSemillero("juan@udea.edu.co", 1L)).isTrue();
    }

    @Test
    @DisplayName("buscarPorId: debe retornar vacío cuando no existe")
    void buscarPorId_inexistente_retornaVacio() {
        when(inscripcionJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(adapter().buscarPorId(99L)).isEmpty();
    }

    @Test
    @DisplayName("buscarPorSemilleroYEstado: traduce estado de dominio a JPA y mapea resultados")
    void buscarPorSemilleroYEstado_traduceYMapea() {
        InscripcionEntity entity = InscripcionEntity.builder()
                .id(1L)
                .semillero(SemilleroEntity.builder().id(1L).nombre("Semillero IA").build())
                .estado(InscripcionEntity.EstadoInscripcionJpa.PENDIENTE)
                .build();

        when(inscripcionJpaRepository.findBySemilleroIdAndEstado(
                eq(1L), eq(InscripcionEntity.EstadoInscripcionJpa.PENDIENTE)))
                .thenReturn(List.of(entity));

        List<Inscripcion> resultado = adapter().buscarPorSemilleroYEstado(
                1L, Inscripcion.EstadoInscripcion.PENDIENTE);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo(Inscripcion.EstadoInscripcion.PENDIENTE);
    }
}
