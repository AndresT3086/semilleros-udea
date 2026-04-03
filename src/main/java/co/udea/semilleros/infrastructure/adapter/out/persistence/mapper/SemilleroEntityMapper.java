package co.udea.semilleros.infrastructure.adapter.out.persistence.mapper;

import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SemilleroEntityMapper {

    @Mapping(target = "idUnidadAcademica", source = "unidadAcademica.id")
    @Mapping(target = "idCampus", source = "campus.id")
    @Mapping(target = "idAreaOcde", source = "areaOcde.id")
    @Mapping(target = "idCoordinador", source = "coordinador.id")
    @Mapping(target = "estado", source = "estado", qualifiedByName = "estadoJpaADominio")
    Semillero toDomain(SemilleroEntity entity);

    @Mapping(target = "unidadAcademica", ignore = true)
    @Mapping(target = "campus", ignore = true)
    @Mapping(target = "areaOcde", ignore = true)
    @Mapping(target = "coordinador", ignore = true)
    @Mapping(target = "estado", source = "estado", qualifiedByName = "estadoDominioAJpa")
    SemilleroEntity toEntity(Semillero domain);

    @Named("estadoJpaADominio")
    default Semillero.EstadoSemillero estadoJpaADominio(SemilleroEntity.EstadoSemilleroJpa estado) {
        if (estado == null) return null;
        return Semillero.EstadoSemillero.valueOf(estado.name());
    }

    @Named("estadoDominioAJpa")
    default SemilleroEntity.EstadoSemilleroJpa estadoDominioAJpa(Semillero.EstadoSemillero estado) {
        if (estado == null) return null;
        return SemilleroEntity.EstadoSemilleroJpa.valueOf(estado.name());
    }
}
