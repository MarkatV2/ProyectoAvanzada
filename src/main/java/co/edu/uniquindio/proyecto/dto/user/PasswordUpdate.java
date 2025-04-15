package co.edu.uniquindio.proyecto.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO utilizado para actualizar la contraseña de un usuario autenticado.
 */
public record PasswordUpdate(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String currentPassword,
        @NotBlank(message = "La contraseña debe contener entre 8 y 50 carácteres")
        @Pattern(regexp = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).*$",
                message = "La contraseña debe contener al menos un dígito, una mayúscula y una minúscula")
        @Size(min = 8, max = 50, message = "La contraseña debe contener entre 8 y 50 carácteres")
        String newPassword
) {}
