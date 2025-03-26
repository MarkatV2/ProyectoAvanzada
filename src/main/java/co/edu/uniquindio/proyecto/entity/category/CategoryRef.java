package co.edu.uniquindio.proyecto.entity.category;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

@Data
public class CategoryRef {
    @Id
    private String id;
    private String name;
}
