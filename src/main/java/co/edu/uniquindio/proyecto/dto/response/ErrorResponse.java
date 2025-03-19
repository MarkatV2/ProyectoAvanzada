package co.edu.uniquindio.proyecto.dto.response;

import java.time.LocalDateTime;

public record ErrorResponse(
        LocalDateTime timestamp,
        String message,
        String code,
        String path) {
}
