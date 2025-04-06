package co.edu.uniquindio.proyecto.entity.report;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

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
