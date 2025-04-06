package co.edu.uniquindio.proyecto.exceptionhandler.comment;

import co.edu.uniquindio.proyecto.dto.response.ErrorResponse;
import co.edu.uniquindio.proyecto.exception.comment.CommentNotFoundExeption;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class CommentExceptionHandler {

    private final ErrorResponseBuilder errorResponseBuilder;

    @ExceptionHandler(CommentNotFoundExeption.class)
    public ResponseEntity<ErrorResponse> handleHistoryNotFound(
            CommentNotFoundExeption ex, WebRequest request) {
        log.error("Error al encontrar el comentario: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

}
