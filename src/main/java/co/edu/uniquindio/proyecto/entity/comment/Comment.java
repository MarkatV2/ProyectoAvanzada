package co.edu.uniquindio.proyecto.entity.comment;

import co.edu.uniquindio.proyecto.util.Ownable;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "comments")
public class Comment implements Ownable {
    @Id
    private ObjectId id;
    private String userName;
    private ObjectId userId;
    private ObjectId reportId;
    private String comment;
    private LocalDateTime createdAt;
    private CommentStatus commentStatus;

    @Override
    public String getUserId() {
        return this.userId.toString();
    }
}
