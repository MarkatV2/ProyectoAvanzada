package co.edu.uniquindio.proyecto.entity.historial;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "status_historials")
@Data
public class StatusHistorial {
    @Id
    private ObjectId id;
    private ObjectId userId;

}
