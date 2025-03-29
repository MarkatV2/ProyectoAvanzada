package co.edu.uniquindio.proyecto.exceptionhandler.auth;

import co.edu.uniquindio.proyecto.dto.response.ErrorResponse;
import co.edu.uniquindio.proyecto.exception.auth.AccountDisabledException;
import co.edu.uniquindio.proyecto.exception.auth.CodeExpiredException;
import co.edu.uniquindio.proyecto.exception.auth.InvalidCodeException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Manejador de excepciones para errores relacionados con la autenticación.
 * Centraliza la generación de respuestas de error utilizando ErrorResponseBuilder.
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class AuthExceptionHandler {

    private final ErrorResponseBuilder errorResponseBuilder;

    /**
     * Maneja la excepción InvalidCodeException.
     *
     * @param ex      Excepción lanzada cuando el código de verificación es inválido.
     * @param request Contexto de la petición.
     * @return ResponseEntity con el error y estado HTTP BAD_REQUEST.
     */
    @ExceptionHandler(InvalidCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCodeException(InvalidCodeException ex, WebRequest request) {
        log.error("Código de verificación inválido: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Maneja la excepción CodeExpiredException.
     *
     * @param ex      Excepción lanzada cuando el código de verificación ha expirado.
     * @param request Contexto de la petición.
     * @return ResponseEntity con el error y estado HTTP BAD_REQUEST.
     */
    @ExceptionHandler(CodeExpiredException.class)
    public ResponseEntity<ErrorResponse> handleCodeExpiredException(CodeExpiredException ex, WebRequest request) {
        log.error("Código de verificación expirado: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Maneja la excepción AccountDisabledException.
     *
     * @param ex      Excepción lanzada cuando la cuenta está deshabilitada.
     * @param request Contexto de la petición.
     * @return ResponseEntity con el error y estado HTTP FORBIDDEN.
     */
    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabledException(AccountDisabledException ex, WebRequest request) {
        log.error("La cuenta está deshabilitada: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.FORBIDDEN, request);
    }
}
