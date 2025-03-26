package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.comment.CommentRequest;
import co.edu.uniquindio.proyecto.dto.comment.CommentResponse;
import co.edu.uniquindio.proyecto.entity.comment.Comment;
import co.edu.uniquindio.proyecto.repository.CommentRepository;
import co.edu.uniquindio.proyecto.service.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImplements{

    private final CommentRepository repository;
    private final CommentMapper mapper;

    public CommentResponse createComment(String reportId, CommentRequest request, String userId, String userName) {
        log.info("Creando un comentario para el reporte: {}", reportId);
        Comment comment = mapper.toEntity(request,reportId,userId,userName);

        comment.setCreatedAt(LocalDateTime.now());

        Comment savedComment = repository.save(comment);
        log.debug("Comentario Creado!!!: id: {}", savedComment.getId());

        return mapper.toResponse(savedComment);
    }

}
