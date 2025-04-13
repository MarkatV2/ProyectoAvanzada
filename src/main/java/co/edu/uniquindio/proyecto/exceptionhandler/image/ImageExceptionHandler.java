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
 * Manejador global de excepciones relacionadas con el procesamiento de imágenes.
 * <p>
 * Este manejador captura excepciones específicas del dominio de imágenes y construye
 * respuestas de error estandarizadas mediante {@link ErrorResponseBuilder}.
 * </p>
 *
 * <p>
 * Las excepciones manejadas incluyen imágenes no encontradas y URLs inválidas.
 * </p>
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ImageExceptionHandler {

    private final ErrorResponseBuilder errorResponseBuilder;

    /**
     * Maneja la excepción {@link ImageNotFoundException} cuando no se encuentra la imagen solicitada.
     *
     * @param ex      Excepción lanzada.
     * @param request Contexto de la petición.
     * @return {@link ResponseEntity} con detalles del error y código 404 (NOT FOUND).
     */
    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleImageNotFound(
            ImageNotFoundException ex, WebRequest request) {
        log.warn("Imagen no encontrada: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    /**
     * Maneja la excepción {@link InvalidImageException} cuando se proporciona una URL o contenido inválido.
     *
     * @param ex      Excepción lanzada.
     * @param request Contexto de la petición.
     * @return {@link ResponseEntity} con detalles del error y código 400 (BAD REQUEST).
     */
    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImage(
            InvalidImageException ex, WebRequest request) {
        log.warn("Imagen inválida: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }
}
