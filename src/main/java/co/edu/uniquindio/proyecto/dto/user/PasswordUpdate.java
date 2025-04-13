package co.edu.uniquindio.proyecto.dto.user;

/**
 * DTO utilizado para actualizar la contraseña de un usuario autenticado.
 */
public record PasswordUpdate(
        String currentPassword,
        String newPassword
) {}
