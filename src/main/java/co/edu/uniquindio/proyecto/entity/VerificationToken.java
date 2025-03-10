package co.edu.uniquindio.proyecto.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "verification_tokens")
@Data @Builder
public class VerificationToken {
    @Id
    private String id;
    private String token;
    private String userId;
    private LocalDateTime expirationDate;

}
