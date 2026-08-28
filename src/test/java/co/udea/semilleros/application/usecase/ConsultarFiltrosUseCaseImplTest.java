package co.udea.semilleros.application.usecase;

import co.udea.semilleros.domain.model.AreaOcde;
import co.udea.semilleros.domain.model.Campus;
import co.udea.semilleros.domain.model.UnidadAcademica;
import co.udea.semilleros.domain.port.out.FiltrosRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarFiltrosUseCase - Pruebas unitarias")
class ConsultarFiltrosUseCaseImplTest {

    @Mock
    private FiltrosRepositoryPort filtrosRepositoryPort;

    @InjectMocks
    private ConsultarFiltrosUseCaseImpl useCase;

    @Test
    @DisplayName("listarUnidadesAcademicas: debe delegar en el puerto y retornar el listado")
    void listarUnidadesAcademicas_retornaListado() {
        // ARRANGE
        UnidadAcademica unidad = UnidadAcademica.builder().id(1L).nombre("Facultad de Ingeniería").build();
        when(filtrosRepositoryPort.listarTodasLasUnidades()).thenReturn(List.of(unidad));

        // ACT
        List<UnidadAcademica> resultado = useCase.listarUnidadesAcademicas();

        // ASSERT
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Facultad de Ingeniería");
    }

    @Test
    @DisplayName("listarCampus: debe delegar en el puerto y retornar el listado")
    void listarCampus_retornaListado() {
        // ARRANGE
        Campus campus = Campus.builder().id(1L).nombre("Ciudad Universitaria").build();
        when(filtrosRepositoryPort.listarTodosLosCampus()).thenReturn(List.of(campus));

        // ACT
        List<Campus> resultado = useCase.listarCampus();

        // ASSERT
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Ciudad Universitaria");
    }

    @Test
    @DisplayName("listarAreasOcde: debe delegar en el puerto y retornar el listado")
    void listarAreasOcde_retornaListado() {
        // ARRANGE
        AreaOcde area = AreaOcde.builder().id(1L).nombre("Ciencias Naturales").build();
        when(filtrosRepositoryPort.listarTodasLasAreas()).thenReturn(List.of(area));

        // ACT
        List<AreaOcde> resultado = useCase.listarAreasOcde();

        // ASSERT
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Ciencias Naturales");
    }
}
