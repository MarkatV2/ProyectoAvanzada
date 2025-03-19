package co.edu.uniquindio.proyecto.dto.category;

public record CategoryResponse(
        String id,
        String name,
        String description,
        String dateCreation // Usamos String para formato ISO-8601
) {}
