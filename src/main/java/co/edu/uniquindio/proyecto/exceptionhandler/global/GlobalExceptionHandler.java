package co.edu.uniquindio.proyecto.exceptionhandler.global;

import co.edu.uniquindio.proyecto.dto.response.ErrorResponse;
import co.edu.uniquindio.proyecto.dto.response.ValidationErrorResponse;
import co.edu.uniquindio.proyecto.exception.global.IdInvalidException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import com.mongodb.MongoSocketOpenException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.naming.ServiceUnavailableException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones. Esta clase captura todas las excepciones
 * no manejadas en los controladores y devuelve respuestas HTTP apropiadas
 * con mensajes detallados sobre el error.
 *
 * <p>La clase maneja una variedad de excepciones, desde errores internos del servidor hasta
 * problemas específicos como validaciones incorrectas, IDs inválidos, problemas de
 * conexión a la base de datos y recursos no encontrados.</p>
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorResponseBuilder errorResponseBuilder;

    /**
     * Maneja la excepción {@link  ServiceUnavailableException}.
     * Esta excepción se utiliza cuando un servicio no está disponible.
     *
     * @param ex      Excepción lanzada cuando el servicio no está disponible.
     * @param request Contexto de la petición.
     * @return Respuesta HTTP con error 503 (Service Unavailable).
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex, WebRequest request) {
        // Log de error detallado
        log.error("Error de servicio no disponible: {}", ex.getMessage(), ex);
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    /**
     * Maneja todas las excepciones generales (Throwable).
     * Se utiliza para capturar cualquier excepción no manejada explícitamente.
     *
     * @param ex      Excepción general capturada.
     * @param request Contexto de la petición.
     * @return Respuesta HTTP con error 500 (Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
        // Log de error detallado
        log.error("Error interno del servidor: {}", ex.getMessage(), ex);
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Maneja las excepciones de validación de parámetros en la entrada de la solicitud.
     * Captura los errores de validación generados por el Bean Validation.
     *
     * @param ex Excepción de validación de parámetros.
     * @return Respuesta HTTP con error 400 (Bad Request) y lista de errores de validación.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ValidationErrorResponse>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Extraer los errores de validación
        List<ValidationErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationErrorResponse(
                        error.getField(), // Nombre del campo
                        error.getDefaultMessage() // Mensaje de error
                ))
                .collect(Collectors.toList());

        // Log de errores de validación
        log.error("Errores de validación encontrados: {}", errors);

        // Retornar la respuesta personalizada con código 400
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    /**
     * Maneja la excepción {@link  IdInvalidException}.
     * Esta excepción se utiliza cuando un identificador no es válido.
     *
     * @param ex      Excepción lanzada cuando el ID es inválido.
     * @param request Contexto de la petición.
     * @return Respuesta HTTP con error 400 (Bad Request).
     */
    @ExceptionHandler(IdInvalidException.class)
    public ResponseEntity<ErrorResponse> handleIdInvalid(IdInvalidException ex, WebRequest request) {
        // Log de error detallado
        log.error("ID inválido: {}", ex.getMessage(), ex);
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Maneja la excepción {@link  MongoSocketOpenException}.
     * Esta excepción ocurre cuando hay un error de conexión con la base de datos MongoDB.
     *
     * @param ex      Excepción lanzada cuando hay un error de conexión con MongoDB.
     * @param request Contexto de la petición.
     * @return Respuesta HTTP con error 503 (Service Unavailable).
     */
    @ExceptionHandler(MongoSocketOpenException.class)
    public ResponseEntity<ErrorResponse> handleMongoSocketOpenException(MongoSocketOpenException ex, WebRequest request) {
        // Log de error detallado
        log.error("Error de conexión a la base de datos MongoDB: {}", ex.getMessage(), ex);
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, request);
    }


    /**
     * Maneja la excepción {@link  NoHandlerFoundException}.
     * Esta excepción ocurre cuando no se encuentra un controlador que maneje la solicitud,
     * es decir, cuando una URL solicitada no está mapeada a ningún recurso.
     *
     * @param ex      Excepción lanzada cuando no se encuentra un controlador para la URL solicitada.
     * @param request Contexto de la petición.
     * @return Respuesta HTTP con error 404 (Not Found).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException ex, WebRequest request) {
        // Log de error detallado
        log.error("Recurso no encontrado para la URL: {}", ex.getRequestURL(), ex);
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Parámetro inválido: {}", ex.getMessage(), ex);
        return ResponseEntity.badRequest().body("ID inválido. Verifica el formato.");
    }


}
