package co.edu.uniquindio.proyecto.DTO;

import java.time.LocalDate;

public record UserResponse(
        String id,
        String email,
        String fullName,
        LocalDate dateBirth,
        String estadoCuenta,
        String cityOfResidence
) {}
