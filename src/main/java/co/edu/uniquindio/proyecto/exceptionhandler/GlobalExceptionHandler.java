package co.edu.uniquindio.proyecto.exceptionhandler;

import co.edu.uniquindio.proyecto.dto.response.ErrorResponse;
import co.edu.uniquindio.proyecto.dto.response.ValidationErrorResponse;
import co.edu.uniquindio.proyecto.exception.*;
import co.edu.uniquindio.proyecto.exception.image.ImageNotFoundException;
import co.edu.uniquindio.proyecto.exception.image.InvalidImageException;
import com.mongodb.MongoSocketOpenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import lombok.extern.slf4j.Slf4j;

import javax.naming.ServiceUnavailableException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


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


    @ExceptionHandler(IdInvalidException.class)
    public ResponseEntity<ErrorResponse> handleIdInvalid(
            IdInvalidException ex, WebRequest request) {
        log.error("Error de ID: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }


    @ExceptionHandler(DuplicateReportException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateReportException(
            DuplicateReportException ex, WebRequest request
    ) {
        log.error("Reporte Duplicado: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReportNotFound(
            ReportNotFoundException ex, WebRequest request) {
        log.error("Error al encontrar Imagen: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }




    @ExceptionHandler(MongoSocketOpenException.class) //Esto debe ir en el seguridad tambien
    public ResponseEntity<ErrorResponse> handleAccountInvalid(
            MongoSocketOpenException ex, WebRequest request) {
        log.error("Error al conectarse en la base de datos {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, request);
    }


    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, HttpStatus status, WebRequest request) {
        return new ResponseEntity<>(
                new ErrorResponse(
                        LocalDateTime.now(),
                        ex.getMessage(),
                        status.getReasonPhrase(),
                        request.getDescription(false),
                        status.value()), status);
    }


}