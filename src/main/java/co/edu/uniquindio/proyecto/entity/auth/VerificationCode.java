package co.edu.uniquindio.proyecto.entity.auth;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "verification_codes")
@Data
public class VerificationCode {
    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String code;

    @Field("user_id")
    private ObjectId userId;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("expires_at")
    private LocalDateTime expiresAt;

}