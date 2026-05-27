package co.udea.semilleros.infrastructure.adapter.out.persistence.repository;

import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemilleroJpaRepository extends JpaRepository<SemilleroEntity, Long> {

    @Query(
            value = """
            SELECT s.* FROM semillero s
            LEFT JOIN unidad_academica ua ON ua.id_unidad = s.id_unidad_academica
            LEFT JOIN campus            c  ON c.id_campus  = s.id_campus
            LEFT JOIN area_ocde         ao ON ao.id_area   = s.id_area_ocde
            WHERE s.estado = 'ACTIVO'
              AND (:idUnidad    IS NULL OR s.id_unidad_academica = :idUnidad)
              AND (:idCampus    IS NULL OR s.id_campus           = :idCampus)
              AND (:idArea      IS NULL OR s.id_area_ocde        = :idArea)
              AND (
                    :palabraClave IS NULL
                    OR unaccent(lower(s.nombre::text))        LIKE unaccent(lower('%' || :palabraClave || '%'))
                    OR unaccent(lower(s.objetivo::text))      LIKE unaccent(lower('%' || :palabraClave || '%'))
                    OR unaccent(lower(s.mision::text))        LIKE unaccent(lower('%' || :palabraClave || '%'))
                    OR unaccent(lower(s.palabras_clave::text)) LIKE unaccent(lower('%' || :palabraClave || '%'))
              )
            ORDER BY s.nombre ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM semillero s
            WHERE s.estado = 'ACTIVO'
              AND (:idUnidad    IS NULL OR s.id_unidad_academica = :idUnidad)
              AND (:idCampus    IS NULL OR s.id_campus           = :idCampus)
              AND (:idArea      IS NULL OR s.id_area_ocde        = :idArea)
              AND (
                    :palabraClave IS NULL
                    OR unaccent(lower(s.nombre::text))        LIKE unaccent(lower('%' || :palabraClave || '%'))
                    OR unaccent(lower(s.objetivo::text))      LIKE unaccent(lower('%' || :palabraClave || '%'))
                    OR unaccent(lower(s.mision::text))        LIKE unaccent(lower('%' || :palabraClave || '%'))
                    OR unaccent(lower(s.palabras_clave::text)) LIKE unaccent(lower('%' || :palabraClave || '%'))
              )
            """,
            nativeQuery = true
    )
    Page<SemilleroEntity> buscarActivos(
            @Param("idUnidad")     Long idUnidad,
            @Param("idCampus")     Long idCampus,
            @Param("idArea")       Long idArea,
            @Param("palabraClave") String palabraClave,
            Pageable pageable
    );

    @Query(value = "SELECT COUNT(*) FROM semillero_integrante WHERE id_semillero = :id AND activo = true", nativeQuery = true)
    Integer contarSemilleristas(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM semillero_actividad WHERE id_semillero = :id AND realiza = true", nativeQuery = true)
    Integer contarActividadesCientificas(@Param("id") Long id);

    Optional<SemilleroEntity> findByCodigo(String codigo);

    List<SemilleroEntity> findByCoordinadorId(Long idCoordinador);

    List<SemilleroEntity> findByCoordinadorIdAndEstadoIn(
            Long idCoordinador,
            List<SemilleroEntity.EstadoSemilleroJpa> estados
    );

    boolean existsByNombre(String nombre);

    boolean existsByCodigo(String codigo);

    long countByEstado(SemilleroEntity.EstadoSemilleroJpa estado);
}
