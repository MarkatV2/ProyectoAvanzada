package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.annotation.CheckOwnerOrAdmin;
import co.edu.uniquindio.proyecto.dto.comment.CommentPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.comment.CommentRequest;
import co.edu.uniquindio.proyecto.dto.comment.CommentResponse;
import co.edu.uniquindio.proyecto.entity.comment.Comment;

/**
 * Servicio para gestionar comentarios relacionados con reportes.
 */
public interface CommentService {

    /**
     * Crea un nuevo comentario.
     *
     * @param request datos del comentario.
     * @return comentario creado.
     */
    CommentResponse createComment(CommentRequest request);

    /**
     * Obtiene un comentario por su ID.
     *
     * @param commentId ID del comentario.
     * @return comentario encontrado.
     */
    CommentResponse getCommentById(String commentId);

    /**
     * Obtiene los comentarios paginados asociados a un reporte.
     *
     * @param reportId ID del reporte.
     * @param page     número de página.
     * @param size     tamaño de página.
     * @return respuesta paginada con los comentarios.
     */
    CommentPaginatedResponse getCommentsByReportId(String reportId, int page, int size);

    /**
     * Elimina un comentario de forma lógica (soft delete).
     *
     * @param commentId ID del comentario.
     * @return comentario eliminado lógicamente.
     */

    @CheckOwnerOrAdmin(entityClass = Comment.class)
    CommentResponse softDeleteComment(String commentId);
}
