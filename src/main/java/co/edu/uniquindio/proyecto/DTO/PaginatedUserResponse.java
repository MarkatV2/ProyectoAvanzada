package co.edu.uniquindio.proyecto.DTO;

import java.util.List;

public record PaginatedUserResponse(
        int totalItems,
        int totalPages,
        int currentPage,
        List<UserResponse> users) {
}
