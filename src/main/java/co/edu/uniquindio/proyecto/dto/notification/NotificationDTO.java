package co.edu.uniquindio.proyecto.dto.notification;

import co.edu.uniquindio.proyecto.entity.notification.NotificationType;
import java.time.LocalDateTime;

/**
 * DTO utilizado para enviar al cliente la información de una notificación recibida.
 * Se transmite típicamente por WebSocket o listado en el historial de notificaciones.
 */
public record NotificationDTO(
        String title,
        String message,
        String reportId,
        NotificationType type,
        LocalDateTime createdAt
) {}

