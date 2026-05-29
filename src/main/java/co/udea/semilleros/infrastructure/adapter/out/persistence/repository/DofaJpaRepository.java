package co.udea.semilleros.infrastructure.adapter.out.persistence.repository;

import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.DofaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DofaJpaRepository extends JpaRepository<DofaEntity, Long> {


    @Query("SELECT d FROM DofaEntity d WHERE d.semillero.id = :idSemillero")
    List<DofaEntity> findBySemilleroId(@Param("idSemillero") Long idSemillero);

    @Modifying
    @Query("DELETE FROM DofaEntity d WHERE d.semillero.id = :idSemillero")
    void deleteBySemilleroId(@Param("idSemillero") Long idSemillero);
}
