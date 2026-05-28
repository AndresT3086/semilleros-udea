package co.udea.semilleros.infrastructure.adapter.out.persistence.repository;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroRelacionamientoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SemilleroRelacionamientoJpaRepository extends JpaRepository<SemilleroRelacionamientoEntity, Long> {

    Optional<SemilleroRelacionamientoEntity> findByIdSemillero(Long idSemillero);
}
