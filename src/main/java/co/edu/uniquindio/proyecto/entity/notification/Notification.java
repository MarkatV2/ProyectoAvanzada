package co.edu.uniquindio.proyecto.entity.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor  // Constructor sin parámetros (por si lo necesitas)
@AllArgsConstructor // Constructor con todos los parámetros
public class Notification {

    @Id
    private ObjectId id;

    private String userId; // ID del receptor de la notificación

    private String title;

    @Field(targetType = FieldType.BOOLEAN)
    private boolean delivered = false;

    private String message;
    private String reportId; // Si está asociada a un reporte
    private NotificationType type;

    private GeoJsonPoint location; // Para filtrar por cercanía

    private boolean read; // Indica si la notificación ha sido leída

    private LocalDateTime createdAt;

}
