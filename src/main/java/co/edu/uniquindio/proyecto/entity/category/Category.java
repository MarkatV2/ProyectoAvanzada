package co.edu.uniquindio.proyecto.entity.category;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "categories")
public class Category {
    @Id
    private ObjectId id;
    private String name;
    private String description;
    private LocalDateTime dateCreation;
    private boolean activated;
}
