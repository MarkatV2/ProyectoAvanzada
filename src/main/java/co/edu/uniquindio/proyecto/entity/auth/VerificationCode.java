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
    private String code;
    private ObjectId userId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private VerificationCodeType verificationCodeType;
}