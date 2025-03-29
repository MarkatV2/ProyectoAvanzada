package co.edu.uniquindio.proyecto.exceptionhandler.auth;

import co.edu.uniquindio.proyecto.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Manejador de errores de seguridad para autenticación y autorización.
 * Implementa tanto {@code AuthenticationEntryPoint} como {@code AccessDeniedHandler} para interceptar
 * fallos de autenticación y denegaciones de acceso, retornando respuestas de error estandarizadas.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Maneja fallos de autenticación.
     *
     * @param request       La petición HTTP.
     * @param response      La respuesta HTTP.
     * @param authException La excepción de autenticación.
     * @throws IOException Si ocurre un error de entrada/salida al escribir la respuesta.
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("Fallo de autenticación en {}: {}", request.getRequestURI(), authException.getMessage());
        ErrorResponse errorResponse = buildAuthError(request, authException);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(errorResponse.status());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Maneja casos de acceso denegado.
     *
     * @param request La petición HTTP.
     * @param response La respuesta HTTP.
     * @param ex La excepción de acceso denegado.
     * @throws IOException Si ocurre un error al escribir la respuesta.
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       org.springframework.security.access.AccessDeniedException ex) throws IOException {
        log.warn("Acceso denegado en {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                "Permisos insuficientes para este recurso",
                "ACCESS_DENIED",
                request.getRequestURI(),
                HttpStatus.FORBIDDEN.value()
        );
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Construye una respuesta de error a partir de una excepción de autenticación.
     * Recorrerá la cadena de excepciones para detectar si el token ha expirado.
     *
     * @param request La petición HTTP.
     * @param ex La excepción de autenticación.
     * @return Un objeto {@code ErrorResponse} con la información de error correspondiente.
     */
    private ErrorResponse buildAuthError(HttpServletRequest request, AuthenticationException ex) {
        log.warn("Procesando excepción de autenticación para {}: {}", request.getRequestURI(), ex.getMessage());
        Throwable cause = ex;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase().contains("expired")) {
                log.debug("Token expirado detectado en la cadena de excepciones para {}.", request.getRequestURI());
                return new ErrorResponse(
                        LocalDateTime.now(),
                        "Token expirado",
                        "TOKEN_EXPIRED",
                        request.getRequestURI(),
                        HttpStatus.UNAUTHORIZED.value()
                );
            }
            cause = cause.getCause();
        }
        return new ErrorResponse(
                LocalDateTime.now(),
                "Error de validación del token",
                "TOKEN_INVALID",
                request.getRequestURI(),
                HttpStatus.UNAUTHORIZED.value()
        );
    }
}
