package co.edu.uniquindio.proyecto.dto.user;

import java.util.List;

/**
 * DTO utilizado para representar una respuesta paginada de usuarios.
 * Se usa comúnmente en vistas administrativas.
 */
public record PaginatedUserResponse(
        int totalItems,
        int totalPages,
        int currentPage,
        List<UserResponse> users) {
}
