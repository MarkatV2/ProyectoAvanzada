package co.edu.uniquindio.proyecto.exceptionhandler;

import co.edu.uniquindio.proyecto.dto.response.ErrorResponse;
import co.edu.uniquindio.proyecto.dto.response.ValidationErrorResponse;
import co.edu.uniquindio.proyecto.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.naming.ServiceUnavailableException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex,  WebRequest request) {
        log.error("Usuario no encontrado: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordException(InvalidPasswordException ex, WebRequest request) {
        log.error("Contraseña actual incorrecta: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex, WebRequest request) {
        log.error("Error de servicio: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    /* @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("Error interno: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    } */

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        // Crear la respuesta personalizada
        ValidationErrorResponse error = new ValidationErrorResponse(
                ex.getName(), // Nombre del parámetro
                "El valor debe ser un número válido" // Mensaje de error
        );

        // Loggear el error
        log.error("Error de tipo en parámetro: {}", error);

        // Retornar la respuesta personalizada con código 400
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex, WebRequest request) {
        log.error("Error de correo ya registrado: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(InvalidCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCodeException(InvalidCodeException ex, WebRequest request) {
        log.error("Código de verificación invalido");
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(CodeExpiredException.class)
    public ResponseEntity<ErrorResponse> handleCodeExpiredException(CodeExpiredException ex, WebRequest request) {
        log.error("Código de verificación expirado");
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ValidationErrorResponse>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Extraer los errores de validación
        List<ValidationErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationErrorResponse(
                        error.getField(), // Nombre del campo
                        error.getDefaultMessage() // Mensaje de error
                ))
                .collect(Collectors.toList());

        // Loggear los errores
        log.error("Errores de validación: {}", errors);

        // Retornar la respuesta personalizada con código 400
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    // Maneja 409 - Conflicto (DuplicateCategoryException)
    @ExceptionHandler(DuplicateCategoryException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCategoryException(
            DuplicateCategoryException ex, WebRequest request
    ) {
        log.error("Categoria Duplicada: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }


    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(
            CategoryNotFoundException ex, WebRequest request) {
        log.error("Error al encontrar categoría: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(IdInvalidException.class)
    public ResponseEntity<ErrorResponse> handleIdInvalid(
            CategoryNotFoundException ex, WebRequest request) {
        log.error("Error de ID: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, HttpStatus status, WebRequest request) {
        return new ResponseEntity<>(
                new ErrorResponse(
                        LocalDateTime.now(),
                        ex.getMessage(),
                        status.getReasonPhrase(),
                        request.getDescription(false)
                ),
                status
        );
    }


}