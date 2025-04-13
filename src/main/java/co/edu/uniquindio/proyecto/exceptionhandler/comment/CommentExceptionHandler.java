package co.edu.uniquindio.proyecto.exceptionhandler.comment;

import co.edu.uniquindio.proyecto.dto.response.ErrorResponse;
import co.edu.uniquindio.proyecto.exception.comment.CommentNotFoundException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Manejador global de excepciones relacionadas con comentarios.
 * <p>
 * Esta clase captura excepciones específicas del dominio de comentarios y genera
 * respuestas de error estandarizadas para el cliente. Se integra con Spring a través
 * de {@link RestControllerAdvice} y utiliza {@link ErrorResponseBuilder} para construir
 * las respuestas de error.
 * </p>
 *
 * <p>
 * Aplica principios SOLID al mantener una única responsabilidad (SRP) y facilita el
 * mantenimiento y la trazabilidad de errores mediante logs estructurados.
 * </p>
 *
 * Ejemplo de excepción manejada: {@link CommentNotFoundException}
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class CommentExceptionHandler {

    private final ErrorResponseBuilder errorResponseBuilder;

    /**
     * Maneja la excepción {@link CommentNotFoundException} cuando no se encuentra
     * un comentario solicitado.
     *
     * @param ex      la excepción lanzada cuando un comentario no se encuentra.
     * @param request la solicitud web que causó la excepción.
     * @return una respuesta HTTP 404 con detalles estructurados del error.
     */
    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFoundException(
            CommentNotFoundException ex, WebRequest request) {

        log.error("Comentario no encontrado. Error: {}. Path: {}",
                ex.getMessage(),
                request.getDescription(false));

        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }
}

