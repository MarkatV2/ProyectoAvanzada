package co.edu.uniquindio.proyecto.dto.comment;

import java.time.LocalDateTime;

public record CommentResponse(
        String id,
        String userName,
        String userId,
        String reportId,
        String comment,
        LocalDateTime createdAt
) {}
