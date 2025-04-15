package co.edu.uniquindio.proyecto.exception;

import io.jsonwebtoken.ExpiredJwtException;

public class RefreshTokenExpiredException extends RuntimeException {
    public RefreshTokenExpiredException(String message, ExpiredJwtException ex) {
        super(message, ex);
    }
}
