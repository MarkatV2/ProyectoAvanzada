package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.comment.CommentPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.comment.CommentRequest;
import co.edu.uniquindio.proyecto.dto.comment.CommentResponse;
import co.edu.uniquindio.proyecto.entity.comment.Comment;
import co.edu.uniquindio.proyecto.entity.comment.CommentStatus;
import co.edu.uniquindio.proyecto.exception.comment.CommentNotFoundExeption;
import co.edu.uniquindio.proyecto.repository.CommentRepository;
import co.edu.uniquindio.proyecto.service.interfaces.CommentService;
import co.edu.uniquindio.proyecto.service.mapper.CommentMapper;
import co.edu.uniquindio.proyecto.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImplements implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final SecurityUtils securityUtils;

    /**
     * Crea un comentario para un reporte.
     *
     * @param request objeto con los datos del comentario.
     * @return CommentResponse con los datos del comentario creado.
     */
    @Transactional
    public CommentResponse createComment(CommentRequest request) {
        log.info("Iniciando creación de comentario para el reporte: {}", request.reportId());

        // Extraer información del usuario autenticado
        String currentUserId = securityUtils.getCurrentUserId();
        // En este ejemplo, también suponemos que el username se encuentra en el token (o se extrae de alguna forma similar)
        String currentUserName = securityUtils.getCurrentUsername();

        Comment comment = commentMapper.toEntity(request, currentUserId, currentUserName);
        Comment saved = commentRepository.save(comment);
        log.info("Comentario creado con ID: {} para el reporte: {}", saved.getId(), saved.getReportId());

        return commentMapper.toResponse(saved);
    }

    /**
     * Busca un comentario por su ID.
     *
     * @param commentId ID del comentario.
     * @return CommentResponse del comentario encontrado.
     * @throws  CommentNotFoundExeption no se encuentra el comentario.
     */
    public CommentResponse getCommentById(String commentId) {
        log.info("Buscando comentario con ID: {}", commentId);
        Comment comment = commentRepository.findById(new ObjectId(commentId))
                .orElseThrow(() -> {
                    log.error("No se encontró comentario con ID: {}", commentId);
                    return new CommentNotFoundExeption("Comentario no encontrado con ID: " + commentId);
                });
        return commentMapper.toResponse(comment);
    }

     /** Obtiene los comentarios de un reporte de forma paginada.
     *
             * @param reportId Identificador del reporte.
            * @param page     Número de página (empezando en 0).
            * @param size     Tamaño de página.
            * @return CommentPaginatedResponse con la lista de comentarios y datos de paginación.
            * @throws CommentNotFoundExeption Si no se encuentran comentarios para el reporte.
     **/
    @Transactional(readOnly = true)
    public CommentPaginatedResponse getCommentsByReportId(String reportId, int page, int size) {
        log.info("Obteniendo comentarios para el reporte con ID: {}", reportId);
        PageRequest pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentRepository.findByReportId(new ObjectId(reportId), pageable);

        if (commentPage.isEmpty()) {
            log.warn("No se encontraron comentarios para el reporte con ID: {}", reportId);
            throw new CommentNotFoundExeption("No se encontraron comentarios para el reporte con ID: " + reportId);
        }

        List<CommentResponse> responses = commentPage.getContent().stream()
                .map(commentMapper::toResponse)
                .collect(Collectors.toList());

        log.info("Se encontraron {} comentarios para el reporte {} en la página {}",
                responses.size(), reportId, page);

        return new CommentPaginatedResponse(
                responses,
                page,
                size,
                commentPage.getTotalElements(),
                commentPage.getTotalPages()
        );
    }

    /**
     * Realiza un soft delete de un comentario, actualizando su estado a ELIMINATED.
     *
     * @param commentId Identificador del comentario.
     * @return CommentResponse con el comentario actualizado.
     * @throws CommentNotFoundExeption Si el comentario no se encuentra.
     */
    @Transactional
    public CommentResponse softDeleteComment(String commentId) {
        log.info("Iniciando soft delete del comentario con ID: {}", commentId);
        Comment comment = commentRepository.findById(new ObjectId(commentId))
                .orElseThrow(() -> {
                    log.error("No se encontró el comentario con ID: {}", commentId);
                    return new CommentNotFoundExeption("Comentario no encontrado con ID: " + commentId);
                });
        // Actualizar el estado a ELIMINATED
        comment.setCommentStatus(CommentStatus.ELIMINATED);
        Comment updated = commentRepository.save(comment);
        log.info("Comentario con ID: {} marcado como ELIMINATED", commentId);
        return commentMapper.toResponse(updated);
    }

}
