package co.edu.uniquindio.proyecto.exception.user;


public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
