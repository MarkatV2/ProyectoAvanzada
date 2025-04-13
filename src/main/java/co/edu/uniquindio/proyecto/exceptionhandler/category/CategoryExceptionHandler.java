package co.edu.uniquindio.proyecto.exceptionhandler.category;

import co.edu.uniquindio.proyecto.dto.response.ErrorResponse;
import co.edu.uniquindio.proyecto.exception.category.CategoryNotFoundException;
import co.edu.uniquindio.proyecto.exception.category.DuplicateCategoryException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Manejador global de excepciones específicas del dominio de categorías.
 * <p>
 * Utiliza {@link ErrorResponseBuilder} para generar respuestas de error consistentes y
 * enriquecidas con detalles como timestamp, código de error y ruta de la petición.
 * </p>
 *
 * <p>
 * Captura y transforma excepciones del dominio de categorías en respuestas HTTP claras,
 * con un formato JSON estandarizado.
 * </p>
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class CategoryExceptionHandler {

    private final ErrorResponseBuilder errorResponseBuilder;

    /**
     * Maneja la excepción {@link DuplicateCategoryException} cuando se intenta crear o actualizar
     * una categoría con un nombre que ya existe.
     *
     * @param ex      Excepción lanzada.
     * @param request Contexto de la petición.
     * @return {@link ResponseEntity} con el detalle del error y código 409 (CONFLICT).
     */
    @ExceptionHandler(DuplicateCategoryException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCategoryException(
            DuplicateCategoryException ex, WebRequest request) {
        log.warn("Intento de crear categoría duplicada: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    /**
     * Maneja la excepción {@link CategoryNotFoundException} cuando no se encuentra
     * la categoría solicitada.
     *
     * @param ex      Excepción lanzada.
     * @param request Contexto de la petición.
     * @return {@link ResponseEntity} con el detalle del error y código 404 (NOT FOUND).
     */
    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(
            CategoryNotFoundException ex, WebRequest request) {
        log.warn("Categoría no encontrada: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }
}
