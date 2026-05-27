package co.udea.semilleros.infrastructure.adapter.out.persistence.repository;

import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.InscripcionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InscripcionJpaRepository extends JpaRepository<InscripcionEntity, Long> {

    @Query("""
            SELECT COUNT(i) > 0 FROM InscripcionEntity i
            WHERE i.correo = :correo
            AND i.semillero.id = :idSemillero
            AND i.estado IN ('PENDIENTE', 'APROBADO')
            """)
    boolean existeInscripcionActivaPorCorreoYSemillero(
            @Param("correo") String correo,
            @Param("idSemillero") Long idSemillero
    );

    List<InscripcionEntity> findBySemilleroIdAndEstado(
            Long idSemillero,
            InscripcionEntity.EstadoInscripcionJpa estado
    );
}
