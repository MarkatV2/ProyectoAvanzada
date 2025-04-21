package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.comment.CommentPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.comment.CommentRequest;
import co.edu.uniquindio.proyecto.dto.comment.CommentResponse;
import co.edu.uniquindio.proyecto.entity.comment.Comment;
import co.edu.uniquindio.proyecto.entity.comment.CommentStatus;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.exception.comment.CommentNotFoundException;
import co.edu.uniquindio.proyecto.exception.global.IdInvalidException;
import co.edu.uniquindio.proyecto.exception.report.ReportNotFoundException;
import co.edu.uniquindio.proyecto.repository.CommentRepository;
import co.edu.uniquindio.proyecto.repository.ReportRepository;
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

/**
 * Servicio para gestionar operaciones relacionadas con comentarios.
 * Implementa la lógica de negocio para crear y obtener comentarios asociados a reportes.
 * <p>
 * Aplica notificaciones al autor del reporte cuando se realiza un nuevo comentario.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final SecurityUtils securityUtils;
    private final ReportRepository reportRepository;
    private final CommentNotificationService commentNotificationService;


    /**
     * Crea un comentario asociado a un reporte.
     * Si el reporte no existe, lanza una excepción.
     * Si el comentario se crea correctamente, se notifica al autor del reporte.
     *
     * @param request objeto con los datos del comentario.
     * @return CommentResponse con los datos del comentario creado.
     * @throws ReportNotFoundException si el reporte no existe.
     */
    @Transactional
    @Override
    public CommentResponse createComment(CommentRequest request) {
        log.info("Iniciando creación de comentario para el reporte con ID: {}", request.reportId());

        Report report = reportRepository.findById(new ObjectId(request.reportId()))
                .orElseThrow(() -> {
                    log.error("No se encontró el reporte con ID: {}", request.reportId());
                    return new ReportNotFoundException(request.reportId());
                });

        String currentUserId = securityUtils.getCurrentUserId();
        String currentUserName = securityUtils.getCurrentUsername();

        Comment comment = commentMapper.toEntity(request, currentUserId, currentUserName);
        Comment savedComment = commentRepository.save(comment);
        log.info("Comentario creado con ID: {} para el reporte: {}", savedComment.getId(), savedComment.getReportId());

        commentNotificationService.notifyOwner(savedComment, report, currentUserName);

        return commentMapper.toResponse(savedComment);
    }


    /**
     * Obtiene un comentario por su ID.
     *
     * @param commentId ID del comentario.
     * @return CommentResponse del comentario encontrado.
     * @throws CommentNotFoundException si el comentario no existe.
     */
    @Override
    public CommentResponse getCommentById(String commentId) {
        log.info("Buscando comentario con ID: {}", commentId);

        Comment comment = commentRepository.findById(new ObjectId(commentId))
                .orElseThrow(() -> {
                    log.error("No se encontró el comentario con ID: {}", commentId);
                    return new CommentNotFoundException("Comentario no encontrado con ID: " + commentId);
                });

        return commentMapper.toResponse(comment);
    }


    /**
     * Obtiene los comentarios asociados a un reporte específico de forma paginada.
     *
     * @param reportId Identificador del reporte.
     * @param page     Número de página (empezando desde 0).
     * @param size     Cantidad de comentarios por página.
     * @return {@link CommentPaginatedResponse} con los comentarios y la información de paginación.
     * @throws IdInvalidException si el reportId no es un ObjectId válido.
     * @throws CommentNotFoundException si no se encuentran comentarios para el reporte.
     */
    @Transactional
    @Override
    public CommentPaginatedResponse getCommentsByReportId(String reportId, int page, int size) {
        log.info("Obteniendo comentarios para el reporte con ID: {} - Página: {}, Tamaño: {}", reportId, page, size);

        ObjectId reportObjectId = parseObjectId(reportId, "ID de reporte inválido: " + reportId);
        PageRequest pageable = PageRequest.of(Math.max(page - 1, 0), size);

        Page<Comment> commentPage = commentRepository.findByAllByReportId(reportObjectId, pageable);

        // <-- Aquí reincorporamos la excepción cuando no hay comentarios
        if (commentPage.isEmpty()) {
            log.warn("No se encontraron comentarios para el reporte con ID: {}", reportId);
            throw new CommentNotFoundException(
                    "No se encontraron comentarios para el reporte con ID: " + reportId
            );
        }

        List<CommentResponse> responses = commentMapper.toResponseList(commentPage.getContent());
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
     * Realiza un borrado lógico de un comentario, marcándolo como ELIMINATED.
     *
     * @param commentId Identificador del comentario.
     * @return {@link CommentResponse} con los datos del comentario actualizado.
     * @throws IdInvalidException si el commentId no es un ObjectId válido.
     * @throws CommentNotFoundException si no se encuentra el comentario.
     */
    @Transactional
    @Override
    public CommentResponse softDeleteComment(String commentId) {
        log.info("Iniciando soft delete del comentario con ID: {}", commentId);

        ObjectId commentObjectId = parseObjectId(commentId, "ID de comentario inválido: " + commentId);

        Comment comment = commentRepository.findById(commentObjectId)
                .orElseThrow(() -> {
                    log.error("No se encontró el comentario con ID: {}", commentId);
                    return new CommentNotFoundException("Comentario no encontrado con ID: " + commentId);
                });

        comment.setCommentStatus(CommentStatus.ELIMINATED);
        Comment updated = commentRepository.save(comment);

        log.info("Comentario con ID: {} marcado como ELIMINATED", commentId);
        return commentMapper.toResponse(updated);
    }

    /**
     * Intenta parsear un string como ObjectId, y lanza una excepción personalizada si falla.
     *
     * @param id         Cadena que representa el ObjectId.
     * @param errorMsg   Mensaje de error personalizado.
     * @return ObjectId válido.
     * @throws IdInvalidException si el id no tiene el formato de un ObjectId válido.
     */
    private ObjectId parseObjectId(String id, String errorMsg) {
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException ex) {
            log.error("Formato inválido de ObjectId: {}", id);
            throw new IdInvalidException(errorMsg);
        }
    }


}
