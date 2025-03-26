package co.edu.uniquindio.proyecto.dto.image;

import org.bson.types.ObjectId;

import java.time.LocalDateTime;

public record ImageResponse(
        String id,
        String imageUrl,
        LocalDateTime uploadDate
) {
}
