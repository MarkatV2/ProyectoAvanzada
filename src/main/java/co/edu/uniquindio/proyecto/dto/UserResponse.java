package co.edu.uniquindio.proyecto.dto;

import java.time.LocalDate;

public record UserResponse(
        String id,
        String email,
        String fullName,
        LocalDate dateBirth,
        String accountStatus,
        String cityOfResidence
) {}
