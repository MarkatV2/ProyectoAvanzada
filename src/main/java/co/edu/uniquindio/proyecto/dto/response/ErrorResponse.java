package co.edu.uniquindio.proyecto.dto.response;

import java.time.LocalDateTime;

/**
 * DTO utilizado para representar una respuesta de error estándar en la API.
 * Proporciona detalles útiles para depurar fallos en las solicitudes.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        String message,
        String code,
        String path,
        int status) {
}
