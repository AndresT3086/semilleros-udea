package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.port.out.SemilleroIntegranteRepositoryPort;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroIntegranteEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.SemilleroIntegranteJpaRepository;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.SemilleroJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SemilleroIntegranteRepositoryAdapter implements SemilleroIntegranteRepositoryPort {

    private final SemilleroIntegranteJpaRepository integranteJpaRepository;
    private final SemilleroJpaRepository semilleroJpaRepository;

    @Override
    public void registrarIntegrante(Long idSemillero, String nombres, String apellidos,
                                    String cedula, String correo, String sexo,
                                    String tipoVinculacion) {
        SemilleroIntegranteEntity integrante = SemilleroIntegranteEntity.builder()
                .semillero(semilleroJpaRepository.getReferenceById(idSemillero))
                .nombres(nombres)
                .apellidos(apellidos)
                .cedula(cedula)
                .correo(correo)
                .sexo(sexo)
                .tipoVinculacion(tipoVinculacion)
                .activo(true)
                .fechaIngreso(LocalDate.now())
                .build();

        integranteJpaRepository.save(integrante);
    }
}
