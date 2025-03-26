package co.edu.uniquindio.proyecto.entity.comment;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "comments")
public class Comment {
    @Id
    private ObjectId id;
    private String userName;
    private ObjectId userId;
    private ObjectId reportId;
    private String comment;
    private LocalDateTime createdAt;
}
