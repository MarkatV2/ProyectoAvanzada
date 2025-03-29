package co.edu.uniquindio.proyecto.exception.auth;

public class InvalidCodeException extends RuntimeException {
    public InvalidCodeException(String message) {
        super(message);
    }

}