package co.udea.semilleros.domain.port.out;

public interface DofaRepositoryPort {

    void guardarDofa(Long idSemillero,
                     String fortalezas,
                     String debilidades,
                     String oportunidades,
                     String amenazas);
}
