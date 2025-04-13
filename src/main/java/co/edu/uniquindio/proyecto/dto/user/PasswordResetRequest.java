package co.edu.uniquindio.proyecto.dto.user;

/**
 * DTO utilizado para confirmar el restablecimiento de contraseña mediante un código.
 */
public record PasswordResetRequest(
        String code,
        String newPassword)
{}

