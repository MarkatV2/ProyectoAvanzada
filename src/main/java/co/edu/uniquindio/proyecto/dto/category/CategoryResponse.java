package co.edu.uniquindio.proyecto.dto.category;

/**
 * DTO utilizado para devolver la información de una categoría al cliente.
 * Incluye metadatos como el identificador y la fecha de creación.
 */
public record CategoryResponse(
        String id,
        String name,
        String description,
        String createdAt
) {}
