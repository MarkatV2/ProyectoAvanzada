package co.edu.uniquindio.proyecto.exceptionhandler;

import co.edu.uniquindio.proyecto.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

/**
 * Componente encargado de construir respuestas de error de forma estandarizada.
 */
@Component
@Slf4j
public class ErrorResponseBuilder {

    /**
     * Construye una respuesta de error a partir de la excepción, el estado HTTP y la petición.
     *
     * @param ex      Excepción que se produjo.
     * @param status  Estado HTTP a retornar.
     * @param request Contexto de la petición.
     * @return ResponseEntity con el objeto ErrorResponse.
     */
    public ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, HttpStatus status, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                ex.getMessage(),
                status.getReasonPhrase(),
                request.getDescription(false),
                status.value()
        );
        return new ResponseEntity<>(errorResponse, status);
    }
}

