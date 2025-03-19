package co.edu.uniquindio.proyecto.dto.user;

public record PasswordUpdate(
        String currentPassword,
        String newPassword
) {}
