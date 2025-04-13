package co.edu.uniquindio.proyecto.exception.user;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String id) {
        super("Usuario no encontrado: " + id);
    }
}
