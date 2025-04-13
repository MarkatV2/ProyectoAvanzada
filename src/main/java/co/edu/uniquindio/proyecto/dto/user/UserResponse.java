package co.edu.uniquindio.proyecto.dto.user;

import java.time.LocalDate;

/**
 * DTO utilizado para representar los datos públicos de un usuario.
 */
public record UserResponse(
        String id,
        String email,
        String fullName,
        LocalDate dateBirth,
        String accountStatus,
        String cityOfResidence,
        double latitude,
        double longitude
) {}
