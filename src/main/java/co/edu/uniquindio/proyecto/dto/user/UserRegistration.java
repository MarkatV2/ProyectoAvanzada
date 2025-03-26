package co.edu.uniquindio.proyecto.dto.user;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record UserRegistration(
        @Email(message = "El email debe tener un formato correcto")
        @Size(min = 8, max = 50, message = "El email debe contener entre 8 y 50 carácteres")
        @NotBlank(message = "El email es obligatorio")
        String email,

        @Size(min = 8, max = 50, message = "La contraseña debe contener entre 8 y 50 carácteres")
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).*$",
                message = "La contraseña debe contener al menos un dígito, una mayúscula y una minúscula")
        String password,

        @Size(min = 8, max = 50, message = "La contraseña debe contener entre 8 y 50 carácteres")
        @NotBlank(message = "El nombre es obligatorio")
        String fullName,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // Formato esperado: yyyy-MM-dd
        @Past(message = "La fecha de nacimiento debe ser en el pasado")
        LocalDate dateBirth,

        @NotBlank(message = "La ciudad de residencia es obligatoria")
        String cityOfResidence,

        @NotNull(message = "La latitud de la dirección es obligatoria")
        double latitude,

        @NotNull(message = "La longitud de la dirección es obligatoria")
        double longitude
) {}