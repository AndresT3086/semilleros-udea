package co.udea.semilleros.domain.port.out;

public interface SemilleroIntegranteRepositoryPort {

    void registrarIntegrante(
            Long idSemillero,
            String nombres,
            String apellidos,
            String cedula,
            String correo,
            String sexo,
            String tipoVinculacion
    );

}
