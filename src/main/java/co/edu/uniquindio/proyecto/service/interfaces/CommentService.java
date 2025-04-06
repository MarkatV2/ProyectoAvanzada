package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.dto.comment.CommentPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.comment.CommentRequest;
import co.edu.uniquindio.proyecto.dto.comment.CommentResponse;

public interface CommentService {
    public CommentResponse createComment(CommentRequest request);
    public CommentResponse getCommentById(String commentId);
    public CommentPaginatedResponse getCommentsByReportId(String reportId, int page, int size);
    public CommentResponse softDeleteComment(String commentId);

}
