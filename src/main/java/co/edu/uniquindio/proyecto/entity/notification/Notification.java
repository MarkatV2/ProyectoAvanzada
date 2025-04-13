package co.edu.uniquindio.proyecto.entity.notification;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Data @Builder
public class Notification {

    @Id
    private String id;

    private String userId; // ID del receptor de la notificación

    private String title;
    private String message;
    private String reportId; // Si está asociada a un reporte
    private NotificationType type;

    private GeoJsonPoint location; // Para filtrar por cercanía

    private boolean read; //false

    private LocalDateTime createdAt;
}
