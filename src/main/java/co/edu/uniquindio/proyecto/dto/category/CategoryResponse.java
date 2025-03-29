package co.edu.uniquindio.proyecto.dto.category;

public record CategoryResponse(
        String id,
        String name,
        String description,
        String createdAt// Usamos String para formato ISO-8601
) {}
