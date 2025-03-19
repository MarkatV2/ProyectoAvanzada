package co.edu.uniquindio.proyecto.dto.category;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank(message = "El nombre de la categoria es obligatorio")
        String name,
        String description
) {}
