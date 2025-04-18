package co.edu.uniquindio.proyecto.exception.user;

import io.jsonwebtoken.ExpiredJwtException;

public class RefreshTokenExpiredException extends RuntimeException {
    public RefreshTokenExpiredException(String message) {
        super(message);
    }
}
