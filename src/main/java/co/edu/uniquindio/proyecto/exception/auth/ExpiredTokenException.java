package co.edu.uniquindio.proyecto.exception.auth;

public class ExpiredTokenException extends TokenException {
    public ExpiredTokenException() {
        super("Token expirado");
    }
}
