package co.edu.uniquindio.proyecto.exception.auth;

public class InvalidTokenException extends TokenException {
    public InvalidTokenException(String message) {
        super("Token inválido: " + message);
    }
}
