package co.edu.uniquindio.proyecto.exceptionhandler.auth;

import co.edu.uniquindio.proyecto.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Manejador de errores de seguridad que intercepta fallos de autenticación y autorización.
 * <p>
 * Implementa {@link AuthenticationEntryPoint} para manejar intentos de acceso no autenticados,
 * e {@link AccessDeniedHandler} para capturar accesos prohibidos a recursos protegidos.
 * Retorna respuestas JSON estandarizadas a través de {@link ErrorResponse}, mejorando la
 * experiencia del cliente y facilitando el diagnóstico de errores.
 * </p>
 *
 * <p>
 * Utiliza {@link ObjectMapper} para serializar la respuesta, e incorpora logs claros con contexto
 * útil para trazabilidad en ambientes productivos.
 * </p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Maneja errores de autenticación, como token ausente, inválido o expirado.
     *
     * @param request       La petición HTTP fallida.
     * @param response      La respuesta HTTP.
     * @param authException La excepción de autenticación lanzada.
     * @throws IOException Si ocurre un error al escribir la respuesta.
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String path = request.getRequestURI();
        log.warn("Fallo de autenticación en [{}]: {}", path, authException.getMessage());

        ErrorResponse errorResponse = buildAuthError(path, authException);
        writeJsonResponse(response, errorResponse);
    }

    /**
     * Maneja errores de autorización (acceso denegado) cuando el usuario autenticado
     * no tiene permisos suficientes.
     *
     * @param request La petición HTTP.
     * @param response La respuesta HTTP.
     * @param ex La excepción de acceso denegado.
     * @throws IOException Si ocurre un error al escribir la respuesta.
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        String path = request.getRequestURI();
        log.warn("Acceso denegado en [{}]: {}", path, ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                "Permisos insuficientes para acceder a este recurso",
                "ACCESS_DENIED",
                path,
                HttpStatus.FORBIDDEN.value()
        );
        writeJsonResponse(response, errorResponse);
    }

    /**
     * Construye una respuesta de error personalizada según el tipo de excepción de autenticación.
     * Detecta si el token ha expirado recorriendo las causas de la excepción.
     *
     * @param path Ruta del recurso al que se intentó acceder.
     * @param ex   Excepción de autenticación lanzada.
     * @return {@link ErrorResponse} con detalles del error.
     */
    private ErrorResponse buildAuthError(String path, AuthenticationException ex) {
        Throwable cause = ex;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase().contains("expired")) {
                log.debug("Token expirado detectado para [{}].", path);
                return new ErrorResponse(
                        LocalDateTime.now(),
                        "El token de autenticación ha expirado",
                        "TOKEN_EXPIRED",
                        path,
                        HttpStatus.UNAUTHORIZED.value()
                );
            }
            cause = cause.getCause();
        }
        return new ErrorResponse(
                LocalDateTime.now(),
                "Token de autenticación inválido o ausente",
                "TOKEN_INVALID",
                path,
                HttpStatus.UNAUTHORIZED.value()
        );
    }

    /**
     * Serializa y escribe un {@link ErrorResponse} como JSON en la respuesta HTTP.
     *
     * @param response      Objeto HTTP donde se escribe.
     * @param errorResponse Contenido del error a devolver.
     * @throws IOException Si ocurre un error de escritura.
     */
    private void writeJsonResponse(HttpServletResponse response, ErrorResponse errorResponse) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(errorResponse.status());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
