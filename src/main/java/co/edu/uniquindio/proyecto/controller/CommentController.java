package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.comment.CommentRequest;
import co.edu.uniquindio.proyecto.dto.comment.CommentResponse;
import co.edu.uniquindio.proyecto.service.implementations.CommentServiceImplements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/comments")
@Slf4j
public class CommentController {

    private final CommentServiceImplements commentService;

    /**
     * Endpoint para crear un comentario.
     *
     * @param request Datos del comentario.
     * @return Respuesta con el comentario creado.
     */
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@Valid @RequestBody CommentRequest request) {
        log.info("Recibiendo solicitud para crear un comentario para el reporte: {}", request.reportId());
        CommentResponse response = commentService.createComment(request);
        log.info("Comentario creado exitosamente con ID: {}", response.id());
        return ResponseEntity.ok(response);
    }


    /**
     * Endpoint para obtener un comentario por su ID.
     *
     * @param commentId ID del comentario.
     * @return ResponseEntity con el comentario encontrado.
     */
    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponse> getCommentById(@PathVariable String commentId) {
        log.info("Recibida petición para obtener comentario con ID: {}", commentId);
        CommentResponse response = commentService.getCommentById(commentId);
        return ResponseEntity.ok(response);
    }


    /**
     * Endpoint para realizar un soft delete de un comentario.
     * Cambia el estado del comentario a ELIMINATED.
     *
     * @param commentId Identificador del comentario.
     * @return ResponseEntity con el comentario actualizado.
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommentResponse> softDeleteComment(@PathVariable String commentId) {
        log.info("Recibida solicitud para eliminar (soft delete) el comentario con ID: {}", commentId);
        CommentResponse response = commentService.softDeleteComment(commentId);
        return ResponseEntity.ok(response);
    }


}

