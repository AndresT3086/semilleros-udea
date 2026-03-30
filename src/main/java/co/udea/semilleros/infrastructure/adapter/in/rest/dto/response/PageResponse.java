package co.udea.semilleros.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PageResponse<T> {
    private List<T> contenido;
    private int paginaActual;
    private int tamano;
    private long totalElementos;
    private int totalPaginas;
    private boolean esUltimaPagina;
    private boolean esPrimeraPagina;
}
