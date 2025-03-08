package co.edu.uniquindio.proyecto.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDate;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String email;
    private String password;
    private String fullName;
    private LocalDate dateBirth;
    private LocalDate dateCreation;
    private Rol rol;
    private AccountStatus accountStatus;
    private String cityOfResidence;
    private String address;
}

