package co.edu.uniquindio.proyecto.dto.image;

import org.bson.types.ObjectId;

import java.time.LocalDateTime;

public record ImageResponse(
        ObjectId id,
        String imageUrl,
        LocalDateTime uploadDate
) {
}
