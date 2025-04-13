package co.edu.uniquindio.proyecto.dto.notification;

import co.edu.uniquindio.proyecto.entity.notification.NotificationType;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

/**
 * DTO utilizado para crear y enviar una notificación a un usuario.
 * Se emplea cuando se genera una notificación a partir de una acción como comentar o crear un reporte.
 */
public record NotificationCreateDTO(
        String userId,
        String title,
        String message,
        String reportId,
        NotificationType type,
        GeoJsonPoint location
) {}
