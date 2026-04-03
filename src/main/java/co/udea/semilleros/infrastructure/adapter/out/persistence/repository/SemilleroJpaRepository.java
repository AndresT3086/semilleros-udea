package co.udea.semilleros.infrastructure.adapter.out.persistence.repository;

import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SemilleroJpaRepository extends JpaRepository<SemilleroEntity, Long> {

    @Query("""
            SELECT s FROM SemilleroEntity s
            LEFT JOIN FETCH s.unidadAcademica ua
            LEFT JOIN FETCH s.campus c
            LEFT JOIN FETCH s.areaOcde a
            WHERE s.estado = 'ACTIVO'
            AND (:idUnidad IS NULL OR ua.id = :idUnidad)
            AND (:idCampus IS NULL OR c.id = :idCampus)
            AND (:idArea IS NULL OR a.id = :idArea)
            AND (:palabraClave IS NULL OR (
                LOWER(s.nombre) LIKE LOWER(CONCAT('%', :palabraClave, '%'))
                OR LOWER(s.objetivo) LIKE LOWER(CONCAT('%', :palabraClave, '%'))
                OR LOWER(s.mision) LIKE LOWER(CONCAT('%', :palabraClave, '%'))
                OR LOWER(s.palabrasClave) LIKE LOWER(CONCAT('%', :palabraClave, '%'))
            ))
            """)
    Page<SemilleroEntity> buscarActivos(
            @Param("idUnidad") Long idUnidad,
            @Param("idCampus") Long idCampus,
            @Param("idArea") Long idArea,
            @Param("palabraClave") String palabraClave,
            Pageable pageable
    );

    Optional<SemilleroEntity> findByCodigo(String codigo);

    Optional<SemilleroEntity> findByCoordinadorId(Long idCoordinador);

    boolean existsByNombre(String nombre);

    boolean existsByCodigo(String codigo);

    long countByEstado(SemilleroEntity.EstadoSemilleroJpa estado);
}
