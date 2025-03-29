package co.edu.uniquindio.proyecto.exception.auth;

public class CodeExpiredException extends RuntimeException {
    public CodeExpiredException(String message) {
        super(message);
    }
}
