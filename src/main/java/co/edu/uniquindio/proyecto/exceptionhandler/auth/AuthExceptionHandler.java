package co.edu.uniquindio.proyecto.exceptionhandler.auth;

import co.edu.uniquindio.proyecto.dto.response.ErrorResponse;
import co.edu.uniquindio.proyecto.exception.user.InvalidRefreshTokenException;
import co.edu.uniquindio.proyecto.exception.user.RefreshTokenExpiredException;
import co.edu.uniquindio.proyecto.exception.auth.AccountDisabledException;
import co.edu.uniquindio.proyecto.exception.auth.CodeExpiredException;
import co.edu.uniquindio.proyecto.exception.auth.InvalidCodeException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
/**
 * Manejador global de excepciones relacionadas con procesos de autenticación.
 * <p>
 * Esta clase captura excepciones específicas del dominio de autenticación como:
 * códigos inválidos, códigos expirados y cuentas deshabilitadas, centralizando
 * la generación de respuestas de error estandarizadas para el cliente.
 * </p>
 *
 * <p>
 * Utiliza {@link ErrorResponseBuilder} para construir respuestas consistentes y
 * {@link RestControllerAdvice} para integrarse con el ciclo de vida de las excepciones
 * en controladores REST. Los errores son registrados usando SLF4J para facilitar
 * la trazabilidad en entornos productivos.
 * </p>
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class AuthExceptionHandler {

    private final ErrorResponseBuilder errorResponseBuilder;

    /**
     * Maneja la excepción {@link InvalidCodeException}, lanzada cuando
     * se proporciona un código de verificación incorrecto.
     *
     * @param ex      Excepción lanzada por un código inválido.
     * @param request Contexto de la petición.
     * @return ResponseEntity con mensaje de error y estado HTTP 400.
     */
    @ExceptionHandler(InvalidCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCodeException(InvalidCodeException ex, WebRequest request) {
        log.error("Código de verificación inválido. Detalles: {} | Ruta: {}", ex.getMessage(), request.getDescription(false));
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Maneja la excepción {@link CodeExpiredException}, lanzada cuando
     * el código de verificación ha caducado.
     *
     * @param ex      Excepción lanzada por código expirado.
     * @param request Contexto de la petición.
     * @return ResponseEntity con mensaje de error y estado HTTP 400.
     */
    @ExceptionHandler(CodeExpiredException.class)
    public ResponseEntity<ErrorResponse> handleCodeExpiredException(CodeExpiredException ex, WebRequest request) {
        log.error("Código de verificación expirado. Detalles: {} | Ruta: {}", ex.getMessage(), request.getDescription(false));
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Maneja la excepción {@link AccountDisabledException}, lanzada cuando
     * un usuario intenta autenticarse con una cuenta deshabilitada.
     *
     * @param ex      Excepción por cuenta inactiva.
     * @param request Contexto de la petición.
     * @return ResponseEntity con mensaje de error y estado HTTP 403.
     */
    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabledException(AccountDisabledException ex, WebRequest request) {
        log.error("La cuenta está deshabilitada. Detalles: {} | Ruta: {}", ex.getMessage(), request.getDescription(false));
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.FORBIDDEN, request);
    }


    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleExpiredRefreshToken( RefreshTokenExpiredException ex, WebRequest request) {
        log.warn("Refresh token expirado: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex, WebRequest request) {
        log.warn("Refresh token inválido: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }
}
