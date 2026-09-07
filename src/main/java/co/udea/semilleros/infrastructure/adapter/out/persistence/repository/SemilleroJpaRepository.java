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
              AND s.estado_caracterizacion = 'COMPLETO'
              AND (:idUnidad    IS NULL OR s.id_unidad_academica = :idUnidad)
              AND (:idCampus    IS NULL OR s.id_campus           = :idCampus)
              AND (:idArea      IS NULL OR s.id_area_ocde        = :idArea)
              AND (
                    :palabraClave IS NULL
                    OR unaccent(lower(s.nombre::text))        LIKE unaccent(lower('%' || :palabraClave || '%'))
                    OR unaccent(lower(s.objetivo::text))      LIKE unaccent(lower('%' || :palabraClave || '%'))
                    OR unaccent(lower(s.mision::text))        LIKE unaccent(lower('%' || :palabraClave || '%'))
                    OR unaccent(lower(s.palabras_clave::text)) LIKE unaccent(lower('%' || :palabraClave || '%'))
                    OR unaccent(lower(ua.nombre::text))         LIKE unaccent(lower('%' || :palabraClave || '%'))
              )
            ORDER BY s.nombre ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM semillero s
            LEFT JOIN unidad_academica ua ON ua.id_unidad = s.id_unidad_academica
            WHERE s.estado = 'ACTIVO'
              AND s.estado_caracterizacion = 'COMPLETO'
              AND (:idUnidad    IS NULL OR s.id_unidad_academica = :idUnidad)
              AND (:idCampus    IS NULL OR s.id_campus           = :idCampus)
              AND (:idArea      IS NULL OR s.id_area_ocde        = :idArea)
              AND (
                    :palabraClave IS NULL
                    OR unaccent(lower(s.nombre::text))        LIKE unaccent(lower('%' || :palabraClave || '%'))
                    OR unaccent(lower(s.objetivo::text))      LIKE unaccent(lower('%' || :palabraClave || '%'))
                    OR unaccent(lower(s.mision::text))        LIKE unaccent(lower('%' || :palabraClave || '%'))
                    OR unaccent(lower(s.palabras_clave::text)) LIKE unaccent(lower('%' || :palabraClave || '%'))
                    OR unaccent(lower(ua.nombre::text))         LIKE unaccent(lower('%' || :palabraClave || '%'))
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

    // ─── Reportes administrativos (solo lectura, datos reales del esquema actual) ──

    @Query(value = """
            SELECT COUNT(*) FROM semillero s
            WHERE s.estado = 'ACTIVO'
              AND (:anioCorte IS NULL OR s.anio_creacion IS NULL OR s.anio_creacion <= :anioCorte)
              AND (:idUnidad  IS NULL OR s.id_unidad_academica = :idUnidad)
              AND (:idCampus  IS NULL OR s.id_campus           = :idCampus)
            """, nativeQuery = true)
    Long contarSemillerosActivosHasta(
            @Param("anioCorte") Integer anioCorte,
            @Param("idUnidad") Long idUnidad,
            @Param("idCampus") Long idCampus
    );

    @Query(value = """
            SELECT COUNT(*) FROM semillero_actividad sa
            JOIN semillero s ON s.id_semillero = sa.id_semillero
            WHERE sa.realiza = true
              AND (:idUnidad IS NULL OR s.id_unidad_academica = :idUnidad)
              AND (:idCampus IS NULL OR s.id_campus           = :idCampus)
            """, nativeQuery = true)
    Long contarActividadesRealizadas(@Param("idUnidad") Long idUnidad, @Param("idCampus") Long idCampus);

    @Query(value = """
            SELECT ua.id_unidad AS id,
                   ua.nombre    AS etiqueta,
                   split_part(ua.nombre, ' ', 1) AS tipo,
                   COUNT(DISTINCT s.id_semillero) AS cantidad_semilleros,
                   COUNT(DISTINCT si.id)          AS cantidad_integrantes
            FROM unidad_academica ua
            LEFT JOIN semillero s
                   ON s.id_unidad_academica = ua.id_unidad
                  AND s.estado = 'ACTIVO'
                  AND (:anioCorte IS NULL OR s.anio_creacion IS NULL OR s.anio_creacion <= :anioCorte)
                  AND (:idCampus  IS NULL OR s.id_campus = :idCampus)
            LEFT JOIN semillero_integrante si
                   ON si.id_semillero = s.id_semillero
                  AND si.activo = true
            GROUP BY ua.id_unidad, ua.nombre
            ORDER BY cantidad_semilleros DESC, ua.nombre ASC
            """, nativeQuery = true)
    List<Object[]> distribucionPorUnidadAcademicaRaw(
            @Param("idCampus") Long idCampus,
            @Param("anioCorte") Integer anioCorte
    );

    @Query(value = """
            SELECT c.id_campus AS id,
                   c.nombre    AS etiqueta,
                   COUNT(DISTINCT s.id_semillero) AS cantidad_semilleros
            FROM campus c
            LEFT JOIN semillero s
                   ON s.id_campus = c.id_campus
                  AND s.estado = 'ACTIVO'
                  AND (:anioCorte IS NULL OR s.anio_creacion IS NULL OR s.anio_creacion <= :anioCorte)
                  AND (:idUnidad  IS NULL OR s.id_unidad_academica = :idUnidad)
            GROUP BY c.id_campus, c.nombre
            ORDER BY cantidad_semilleros DESC, c.nombre ASC
            """, nativeQuery = true)
    List<Object[]> distribucionPorCampusRaw(
            @Param("idUnidad") Long idUnidad,
            @Param("anioCorte") Integer anioCorte
    );

    @Query(value = """
            SELECT s.anio_creacion AS anio, COUNT(*) AS cantidad
            FROM semillero s
            WHERE s.estado = 'ACTIVO'
              AND s.anio_creacion IS NOT NULL
              AND (:idUnidad IS NULL OR s.id_unidad_academica = :idUnidad)
              AND (:idCampus IS NULL OR s.id_campus           = :idCampus)
            GROUP BY s.anio_creacion
            ORDER BY s.anio_creacion ASC
            """, nativeQuery = true)
    List<Object[]> conteoPorAnioCreacionRaw(@Param("idUnidad") Long idUnidad, @Param("idCampus") Long idCampus);

    @Query(
            value = """
            SELECT s.id_semillero AS id,
                   s.nombre        AS nombre,
                   s.codigo        AS codigo,
                   ua.nombre       AS unidadAcademica,
                   split_part(ua.nombre, ' ', 1) AS tipoUnidad,
                   c.nombre        AS campus,
                   COALESCE(pi.cnt, 0) AS participantes,
                   COALESCE(pa.cnt, 0) AS actividadesRealizadas,
                   s.estado        AS estado,
                   s.anio_creacion AS anioCreacion
            FROM semillero s
            LEFT JOIN unidad_academica ua ON ua.id_unidad = s.id_unidad_academica
            LEFT JOIN campus c            ON c.id_campus  = s.id_campus
            LEFT JOIN (
                SELECT id_semillero, COUNT(*) cnt FROM semillero_integrante WHERE activo = true GROUP BY id_semillero
            ) pi ON pi.id_semillero = s.id_semillero
            LEFT JOIN (
                SELECT id_semillero, COUNT(*) cnt FROM semillero_actividad WHERE realiza = true GROUP BY id_semillero
            ) pa ON pa.id_semillero = s.id_semillero
            WHERE s.estado = 'ACTIVO'
              AND (:idUnidad    IS NULL OR s.id_unidad_academica = :idUnidad)
              AND (:idCampus    IS NULL OR s.id_campus           = :idCampus)
              AND (:idSemillero IS NULL OR s.id_semillero        = :idSemillero)
              AND (:anioCorte   IS NULL OR s.anio_creacion IS NULL OR s.anio_creacion <= :anioCorte)
            """,
            countQuery = """
            SELECT COUNT(*) FROM semillero s
            WHERE s.estado = 'ACTIVO'
              AND (:idUnidad    IS NULL OR s.id_unidad_academica = :idUnidad)
              AND (:idCampus    IS NULL OR s.id_campus           = :idCampus)
              AND (:idSemillero IS NULL OR s.id_semillero        = :idSemillero)
              AND (:anioCorte   IS NULL OR s.anio_creacion IS NULL OR s.anio_creacion <= :anioCorte)
            """,
            nativeQuery = true
    )
    Page<Object[]> rendimientoPorSemilleroRaw(
            @Param("idUnidad") Long idUnidad,
            @Param("idCampus") Long idCampus,
            @Param("idSemillero") Long idSemillero,
            @Param("anioCorte") Integer anioCorte,
            Pageable pageable
    );

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
