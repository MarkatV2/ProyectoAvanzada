package co.edu.uniquindio.proyecto.entity.image;

import co.edu.uniquindio.proyecto.util.Ownable;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "images")
@Data
public class Image implements Ownable {
    @Id
    ObjectId id;
    String imageUrl;
    LocalDateTime uploadDate;
    ObjectId reportId;
    ObjectId userId;

    @Override
    public String getUserId(){
        return this.userId.toString();
    };

}
