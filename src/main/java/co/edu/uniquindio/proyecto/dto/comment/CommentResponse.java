package co.edu.uniquindio.proyecto.dto.comment;

import java.time.LocalDateTime;

/**
 * DTO utilizado para devolver los datos de un comentario.
 * Incluye información del autor, el reporte relacionado y la fecha de creación.
 */
public record CommentResponse(
        String id,
        String userName,
        String userId,
        String reportId,
        String comment,
        LocalDateTime createdAt
) {}
