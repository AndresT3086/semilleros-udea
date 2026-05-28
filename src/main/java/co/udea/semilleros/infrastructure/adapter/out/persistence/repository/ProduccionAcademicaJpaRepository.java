package co.udea.semilleros.infrastructure.adapter.out.persistence.repository;

import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.ProduccionAcademicaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProduccionAcademicaJpaRepository extends JpaRepository<ProduccionAcademicaEntity, Long> {

    Optional<ProduccionAcademicaEntity> findByIdSemillero(Long idSemillero);

}
