package co.edu.uniquindio.proyecto.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El email no debe estar vacío")
        @Email(message = "El email debe tener un formato válido")
        String userName,

        @NotBlank(message = "La contraseña no debe estar vacía")
        String password
) {
}
