package co.udea.semilleros.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PageResult<T> {
    private final List<T> contenido;
    private final int paginaActual;
    private final int tamano;
    private final long totalElementos;
    private final int totalPaginas;
    private final boolean esUltimaPagina;
    private final boolean esPrimeraPagina;
}
