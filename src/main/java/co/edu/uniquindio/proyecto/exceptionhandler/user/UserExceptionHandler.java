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
 * Manejador global de excepciones relacionadas con operaciones de usuario.
 * <p>
 * Se encarga de interceptar excepciones comunes del dominio de usuario y generar
 * respuestas estándar usando {@link ErrorResponseBuilder}.
 * </p>
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class UserExceptionHandler {

    private final ErrorResponseBuilder errorResponseBuilder;

    /**
     * Maneja la excepción {@link UserNotFoundException} cuando no se encuentra el usuario solicitado.
     *
     * @param ex      Excepción lanzada.
     * @param request Contexto de la petición.
     * @return {@link ResponseEntity} con código 404 (NOT FOUND).
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(
            UserNotFoundException ex, WebRequest request) {
        log.warn("Usuario no encontrado: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    /**
     * Maneja la excepción {@link InvalidPasswordException} cuando la contraseña actual proporcionada no es válida.
     *
     * @param ex      Excepción lanzada.
     * @param request Contexto de la petición.
     * @return {@link ResponseEntity} con código 400 (BAD REQUEST).
     */
    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordException(
            InvalidPasswordException ex, WebRequest request) {
        log.warn("Contraseña actual incorrecta: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Maneja la excepción {@link EmailAlreadyExistsException} cuando el correo electrónico ya está registrado.
     *
     * @param ex      Excepción lanzada.
     * @param request Contexto de la petición.
     * @return {@link ResponseEntity} con código 409 (CONFLICT).
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex, WebRequest request) {
        log.warn("Correo electrónico ya registrado: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }
}
