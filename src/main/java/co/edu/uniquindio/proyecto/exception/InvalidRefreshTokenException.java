package co.edu.uniquindio.proyecto.exception;

import org.springframework.security.oauth2.jwt.JwtException;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message, JwtException ex) {
        super(message, ex);
    }
}
