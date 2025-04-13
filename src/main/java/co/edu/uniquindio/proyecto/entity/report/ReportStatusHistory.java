package co.edu.uniquindio.proyecto.entity.report;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Representa el historial de cambios de estado de un reporte en la base de datos.
 * Cada instancia de esta clase representa un cambio de estado que ha tenido un reporte en un momento dado,
 * incluyendo el estado anterior, el estado nuevo, y la información del usuario que realizó el cambio.
 */
@Document(collection = "report_status_histories")
@Data
public class ReportStatusHistory {

    @Id
    private ObjectId id;
    private ObjectId reportId;
    private ObjectId userId;
    private ReportStatus previousStatus;
    private ReportStatus newStatus;
    private LocalDateTime changedAt;

}
