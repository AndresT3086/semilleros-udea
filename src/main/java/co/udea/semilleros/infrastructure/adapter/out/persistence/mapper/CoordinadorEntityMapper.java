package co.udea.semilleros.infrastructure.adapter.out.persistence.mapper;

import co.udea.semilleros.domain.model.Coordinador;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.CoordinadorEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CoordinadorEntityMapper {

    Coordinador toDomain(CoordinadorEntity entity);

    CoordinadorEntity toEntity(Coordinador domain);
}
