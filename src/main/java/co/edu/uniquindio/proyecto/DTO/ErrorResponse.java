package co.edu.uniquindio.proyecto.DTO;

import java.time.LocalDateTime;

public record ErrorResponse(
        LocalDateTime timestamp,
        String message,
        String code,
        String path) {
}
