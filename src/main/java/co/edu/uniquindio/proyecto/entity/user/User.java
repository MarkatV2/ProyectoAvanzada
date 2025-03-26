package co.edu.uniquindio.proyecto.entity.user;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(collection = "users")
public class User {
    @Id
    private ObjectId id;
    private String email;
    private String password;
    private String fullName;
    private LocalDateTime dateBirth;
    private LocalDateTime createdAt;
    private Rol rol;
    private AccountStatus accountStatus;
    private String cityOfResidence;
    private String latitude;
    private String longitude;
}

