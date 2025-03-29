package co.edu.uniquindio.proyecto.exceptionhandler.image;

import co.edu.uniquindio.proyecto.dto.response.ErrorResponse;
import co.edu.uniquindio.proyecto.exception.image.ImageNotFoundException;
import co.edu.uniquindio.proyecto.exception.image.InvalidImageException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Manejador de excepciones para errores relacionados con imágenes.
 * <p>
 * Centraliza la generación de respuestas de error utilizando ErrorResponseBuilder.
 * </p>
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ImageExceptionHandler {

    private final ErrorResponseBuilder errorResponseBuilder;

    /**
     * Maneja la excepción {@code ImageNotFoundException}.
     *
     * @param ex      La excepción lanzada cuando no se encuentra una imagen.
     * @param request Contexto de la petición.
     * @return ResponseEntity con el ErrorResponse y estado HTTP 404 (Not Found).
     */
    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleImageNotFound(
            ImageNotFoundException ex, WebRequest request) {
        log.error("Error al encontrar imagen: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    /**
     * Maneja la excepción {@code InvalidImageException}.
     *
     * @param ex      La excepción lanzada cuando la URL de la imagen es inválida.
     * @param request Contexto de la petición.
     * @return ResponseEntity con el ErrorResponse y estado HTTP 400 (Bad Request).
     */
    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImage(
            InvalidImageException ex, WebRequest request) {
        log.error("Error de imagen (URL no válida): {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }
}
