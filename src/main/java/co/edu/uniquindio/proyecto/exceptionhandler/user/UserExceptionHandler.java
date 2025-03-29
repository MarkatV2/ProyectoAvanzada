package co.edu.uniquindio.proyecto.exceptionhandler.user;

import co.edu.uniquindio.proyecto.dto.response.ErrorResponse;
import co.edu.uniquindio.proyecto.exception.user.EmailAlreadyExistsException;
import co.edu.uniquindio.proyecto.exception.user.InvalidPasswordException;
import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Manejador de excepciones para controladores de usuario.
 * Centraliza la generación de respuestas de error utilizando el ErrorResponseBuilder.
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class UserExceptionHandler {

    private final ErrorResponseBuilder errorResponseBuilder;

    /**
     * Maneja la excepción de usuario no encontrado.
     *
     * @param ex      Excepción lanzada.
     * @param request Contexto de la petición.
     * @return Respuesta HTTP con error 404.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        log.error("Usuario no encontrado: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    /**
     * Maneja la excepción de contraseña incorrecta.
     *
     * @param ex      Excepción lanzada.
     * @param request Contexto de la petición.
     * @return Respuesta HTTP con error 400.
     */
    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordException(InvalidPasswordException ex, WebRequest request) {
        log.error("Contraseña actual incorrecta: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Maneja la excepción de correo ya registrado.
     *
     * @param ex      Excepción lanzada.
     * @param request Contexto de la petición.
     * @return Respuesta HTTP con error 409.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex, WebRequest request) {
        log.error("Error de correo ya registrado: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }
}
