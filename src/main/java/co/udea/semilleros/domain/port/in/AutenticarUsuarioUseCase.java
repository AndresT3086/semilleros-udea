package co.udea.semilleros.domain.port.in;

public interface AutenticarUsuarioUseCase {

    String autenticar(String correo, String password, int respuestaMath, int operando1, int operando2);
}
