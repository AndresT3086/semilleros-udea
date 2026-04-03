package co.udea.semilleros.infrastructure.adapter.out.persistence.mapper;

import co.udea.semilleros.domain.model.Inscripcion;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.InscripcionEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InscripcionEntityMapper {

    @Mapping(target = "idSemillero", source = "semillero.id")
    @Mapping(target = "nombreSemillero", source = "semillero.nombre")
    @Mapping(target = "estado", source = "estado", qualifiedByName = "estadoJpaADominio")
    Inscripcion toDomain(InscripcionEntity entity);

    @Mapping(target = "semillero", ignore = true)
    @Mapping(target = "estado", source = "estado", qualifiedByName = "estadoDominioAJpa")
    InscripcionEntity toEntity(Inscripcion domain);

    @Named("estadoJpaADominio")
    default Inscripcion.EstadoInscripcion estadoJpaADominio(InscripcionEntity.EstadoInscripcionJpa estado) {
        if (estado == null) return null;
        return Inscripcion.EstadoInscripcion.valueOf(estado.name());
    }

    @Named("estadoDominioAJpa")
    default InscripcionEntity.EstadoInscripcionJpa estadoDominioAJpa(Inscripcion.EstadoInscripcion estado) {
        if (estado == null) return null;
        return InscripcionEntity.EstadoInscripcionJpa.valueOf(estado.name());
    }
}
