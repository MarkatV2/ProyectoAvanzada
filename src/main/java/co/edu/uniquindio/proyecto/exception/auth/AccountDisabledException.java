package co.edu.uniquindio.proyecto.exception.auth;

public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException(String message) {
        super(message);
    }
}