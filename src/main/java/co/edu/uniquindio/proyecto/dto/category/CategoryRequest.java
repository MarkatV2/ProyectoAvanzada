package co.edu.uniquindio.proyecto.dto.category;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO utilizado para crear o actualizar una categoría.
 * Contiene los datos requeridos desde el cliente.
 */
public record CategoryRequest(

        @NotBlank(message = "El nombre de la categoría es obligatorio")
        String name,

        String description
) {}