package co.edu.uniquindio.proyecto.dto.image;

import org.bson.types.ObjectId;

import java.time.LocalDateTime;

/**
 * DTO utilizado para devolver la información de una imagen registrada en el sistema.
 */
public record ImageResponse(
        String id,
        String imageUrl,
        LocalDateTime uploadDate
) {
}
