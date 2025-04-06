package co.edu.uniquindio.proyecto.dto.comment;

import java.util.List;

/**
 * DTO para representar una respuesta paginada de comentarios.
 *
 * @param content       Lista de comentarios.
 * @param page          Número de página actual.
 * @param size          Tamaño de página.
 * @param totalElements Total de elementos encontrados.
 * @param totalPages    Total de páginas.
 */
public record CommentPaginatedResponse(
        List<CommentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}

