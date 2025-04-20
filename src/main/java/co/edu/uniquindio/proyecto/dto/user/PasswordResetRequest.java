package co.edu.uniquindio.proyecto.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO utilizado para confirmar el restablecimiento de contraseña mediante un código.
 */
public record PasswordResetRequest(
        @NotBlank(message = "el código de verificación es obligatorio")
        String code,
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).*$",
                message = "La contraseña debe contener al menos un dígito, una mayúscula y una minúscula")
        String newPassword)
{}

