package co.udea.semilleros.infrastructure.adapter.out.persistence;

import co.udea.semilleros.domain.model.PageResult;
import co.udea.semilleros.domain.model.Semillero;
import co.udea.semilleros.domain.model.SemilleroFiltro;
import co.udea.semilleros.domain.port.out.SemilleroRepositoryPort;
import co.udea.semilleros.infrastructure.adapter.out.persistence.entity.SemilleroEntity;
import co.udea.semilleros.infrastructure.adapter.out.persistence.mapper.SemilleroEntityMapper;
import co.udea.semilleros.infrastructure.adapter.out.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SemilleroRepositoryAdapter implements SemilleroRepositoryPort {

    private final SemilleroJpaRepository semilleroJpaRepository;
    private final SemilleroEntityMapper semilleroEntityMapper;
    private final UnidadAcademicaJpaRepository unidadAcademicaJpaRepository;
    private final CampusJpaRepository campusJpaRepository;
    private final AreaOcdeJpaRepository areaOcdeJpaRepository;
    private final CoordinadorJpaRepository coordinadorJpaRepository;

    @Override
    public PageResult<Semillero> buscarActivos(SemilleroFiltro filtro) {
        int pagina = filtro.getPagina() != null ? filtro.getPagina() : 0;
        int tamano = filtro.getTamano() != null ? filtro.getTamano() : 15;

        PageRequest pageRequest = PageRequest.of(pagina, tamano);

        Page<SemilleroEntity> page = semilleroJpaRepository.buscarActivos(
                filtro.getIdUnidadAcademica(),
                filtro.getIdCampus(),
                filtro.getIdAreaOcde(),
                normalizarPalabraClave(filtro.getPalabraClave()),
                pageRequest
        );

        List<Semillero> contenido = page.getContent().stream()
                .map(this::toDomainConConteos)
                .toList();

        return PageResult.<Semillero>builder()
                .contenido(contenido)
                .paginaActual(page.getNumber())
                .tamano(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .esUltimaPagina(page.isLast())
                .esPrimeraPagina(page.isFirst())
                .build();
    }

    @Override
    public Optional<Semillero> buscarPorId(Long id) {
        return semilleroJpaRepository.findById(id)
                .map(this::toDomainConConteos);
    }

    @Override
    public Optional<Semillero> buscarPorCodigo(String codigo) {
        return semilleroJpaRepository.findByCodigo(codigo)
                .map(semilleroEntityMapper::toDomain);
    }

    @Override
    public Optional<Semillero> buscarPorCoordinador(Long idCoordinador) {
        return semilleroJpaRepository.findByCoordinadorId(idCoordinador)
                .map(semilleroEntityMapper::toDomain);
    }

    @Override
    public Semillero guardar(Semillero semillero) {
        SemilleroEntity entity = construirEntidad(semillero);
        return semilleroEntityMapper.toDomain(semilleroJpaRepository.save(entity));
    }

    @Override
    public boolean existePorNombre(String nombre) {
        return semilleroJpaRepository.existsByNombre(nombre);
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        return semilleroJpaRepository.existsByCodigo(codigo);
    }

    @Override
    public long contarPorEstado(Semillero.EstadoSemillero estado) {
        return semilleroJpaRepository.countByEstado(
                SemilleroEntity.EstadoSemilleroJpa.valueOf(estado.name()));
    }

    private Semillero toDomainConConteos(SemilleroEntity entity) {
        Semillero base = semilleroEntityMapper.toDomain(entity);

        Integer semilleristas = semilleroJpaRepository.contarSemilleristas(entity.getId());
        Integer actividades   = semilleroJpaRepository.contarActividadesCientificas(entity.getId());

        return base
                .withTotalSemilleristas(semilleristas != null ? semilleristas : 0)
                .withTotalActividadesCientificas(actividades != null ? actividades : 0);
    }

    private SemilleroEntity construirEntidad(Semillero semillero) {
        SemilleroEntity entity = semilleroEntityMapper.toEntity(semillero);

        if (semillero.getId() != null) {
            semilleroJpaRepository.findById(semillero.getId())
                    .ifPresent(existente -> entity.setFechaCreacion(existente.getFechaCreacion()));
        }

        resolverRelaciones(entity, semillero);
        return entity;
    }

    private void resolverRelaciones(SemilleroEntity entity, Semillero semillero) {
        if (semillero.getIdUnidadAcademica() != null) {
            unidadAcademicaJpaRepository.findById(semillero.getIdUnidadAcademica())
                    .ifPresent(entity::setUnidadAcademica);
        }
        if (semillero.getIdCampus() != null) {
            campusJpaRepository.findById(semillero.getIdCampus())
                    .ifPresent(entity::setCampus);
        }
        if (semillero.getIdAreaOcde() != null) {
            areaOcdeJpaRepository.findById(semillero.getIdAreaOcde())
                    .ifPresent(entity::setAreaOcde);
        }
        if (semillero.getIdCoordinador() != null) {
            coordinadorJpaRepository.findById(semillero.getIdCoordinador())
                    .ifPresent(entity::setCoordinador);
        }
    }

    private String normalizarPalabraClave(String palabraClave) {
        if (palabraClave == null || palabraClave.isBlank()) {
            return null;
        }
        return palabraClave.trim();
    }
}
