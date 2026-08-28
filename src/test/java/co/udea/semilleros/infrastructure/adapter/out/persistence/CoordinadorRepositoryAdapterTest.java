package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.Coordinador;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.CoordinadorEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.mapper.CoordinadorEntityMapperImpl;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.CoordinadorJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CoordinadorRepositoryAdapter - Pruebas unitarias")
class CoordinadorRepositoryAdapterTest {

    @Mock private CoordinadorJpaRepository coordinadorJpaRepository;

    private final CoordinadorEntityMapperImpl mapper = new CoordinadorEntityMapperImpl();

    private CoordinadorRepositoryAdapter adapter() {
        return new CoordinadorRepositoryAdapter(coordinadorJpaRepository, mapper);
    }

    @Test
    @DisplayName("buscarPorCorreo: debe mapear a dominio cuando existe")
    void buscarPorCorreo_existente_retornaCoordinador() {
        CoordinadorEntity entity = CoordinadorEntity.builder()
                .id(1L).correo("coordinador@udea.edu.co").nombres("Ana").apellidos("Gómez").build();
        when(coordinadorJpaRepository.findByCorreo("coordinador@udea.edu.co")).thenReturn(Optional.of(entity));

        Optional<Coordinador> resultado = adapter().buscarPorCorreo("coordinador@udea.edu.co");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNombres()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("buscarPorCorreo: debe retornar vacío cuando no existe")
    void buscarPorCorreo_inexistente_retornaVacio() {
        when(coordinadorJpaRepository.findByCorreo("no@udea.edu.co")).thenReturn(Optional.empty());

        assertThat(adapter().buscarPorCorreo("no@udea.edu.co")).isEmpty();
    }

    @Test
    @DisplayName("buscarPorId: debe mapear a dominio cuando existe")
    void buscarPorId_existente_retornaCoordinador() {
        CoordinadorEntity entity = CoordinadorEntity.builder().id(1L).correo("c@udea.edu.co").build();
        when(coordinadorJpaRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThat(adapter().buscarPorId(1L)).isPresent();
    }

    @Test
    @DisplayName("guardar: debe mapear a entidad, guardar y remapear a dominio")
    void guardar_mapeaGuardaYRemapea() {
        Coordinador dominio = Coordinador.builder().nombres("Ana").correo("ana@udea.edu.co").build();
        CoordinadorEntity guardada = CoordinadorEntity.builder().id(5L).nombres("Ana").correo("ana@udea.edu.co").build();

        when(coordinadorJpaRepository.save(any())).thenReturn(guardada);

        Coordinador resultado = adapter().guardar(dominio);

        assertThat(resultado.getId()).isEqualTo(5L);
    }
}
