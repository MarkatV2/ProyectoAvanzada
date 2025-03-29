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
 * Manejador de excepciones para errores relacionados con categorías.
 * Centraliza la generación de respuestas de error utilizando ErrorResponseBuilder.
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class CategoryExceptionHandler {

    private final ErrorResponseBuilder errorResponseBuilder;

    /**
     * Maneja la excepción DuplicateCategoryException.
     *
     * @param ex      Excepción lanzada cuando se intenta crear o actualizar una categoría con un nombre duplicado.
     * @param request Contexto de la petición.
     * @return ResponseEntity con el error y estado HTTP 409 (CONFLICT).
     */
    @ExceptionHandler(DuplicateCategoryException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCategoryException(
            DuplicateCategoryException ex, WebRequest request) {
        log.error("Categoría duplicada: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    /**
     * Maneja la excepción CategoryNotFoundException.
     *
     * @param ex      Excepción lanzada cuando no se encuentra una categoría.
     * @param request Contexto de la petición.
     * @return ResponseEntity con el error y estado HTTP 404 (NOT FOUND).
     */
    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(
            CategoryNotFoundException ex, WebRequest request) {
        log.error("Categoría no encontrada: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }
}
