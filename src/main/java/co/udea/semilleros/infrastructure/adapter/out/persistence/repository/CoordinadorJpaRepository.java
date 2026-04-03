package co.udea.semilleros.infrastructure.adapter.out.persistence.repository;

import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.CoordinadorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoordinadorJpaRepository extends JpaRepository<CoordinadorEntity, Long> {

    Optional<CoordinadorEntity> findByCorreo(String correo);
}
