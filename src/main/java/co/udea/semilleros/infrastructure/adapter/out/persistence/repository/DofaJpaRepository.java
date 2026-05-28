package co.udea.semilleros.infrastructure.adapter.out.persistence.repository;

import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.DofaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DofaJpaRepository extends JpaRepository<DofaEntity, Long> {

    @Modifying
    @Query("DELETE FROM DofaEntity d WHERE d.semillero.id = :idSemillero")
    void deleteByIdSemillero(@Param("idSemillero") Long idSemillero);
}
