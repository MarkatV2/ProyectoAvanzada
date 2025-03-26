package co.edu.uniquindio.proyecto.entity.image;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "images")
@Data
public class Image {
    @Id
    ObjectId id;
    String imageUrl;
    LocalDateTime uploadDate;
    ObjectId reportId;
}
