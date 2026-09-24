package co.udea.semilleros.infrastructure.adapter.out.persistence.repository;

import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroIntegranteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface SemilleroIntegranteJpaRepository extends JpaRepository<SemilleroIntegranteEntity, Long> {

    @Query(value = """
            SELECT COUNT(*) FROM semillero_integrante si
            JOIN semillero s ON s.id_semillero = si.id_semillero
            WHERE (:fechaCorte IS NULL OR si.fecha_ingreso IS NULL OR si.fecha_ingreso <= :fechaCorte)
              AND (:idUnidad IS NULL OR s.id_unidad_academica = :idUnidad)
              AND (:idCampus IS NULL OR s.id_campus           = :idCampus)
            """, nativeQuery = true)
    Long contarRegistradosHasta(
            @Param("fechaCorte") LocalDate fechaCorte,
            @Param("idUnidad") Long idUnidad,
            @Param("idCampus") Long idCampus
    );

    @Query(value = """
            SELECT COUNT(*) FROM semillero_integrante si
            JOIN semillero s ON s.id_semillero = si.id_semillero
            WHERE si.activo = true
              AND (:idUnidad IS NULL OR s.id_unidad_academica = :idUnidad)
              AND (:idCampus IS NULL OR s.id_campus           = :idCampus)
            """, nativeQuery = true)
    Long contarActivos(@Param("idUnidad") Long idUnidad, @Param("idCampus") Long idCampus);
}
