package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.comment.CommentRequest;
import co.edu.uniquindio.proyecto.dto.comment.CommentResponse;
import co.edu.uniquindio.proyecto.service.interfaces.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Controlador REST para la gestión de comentarios en reportes.
 * <p>
 * Permite a los usuarios crear, consultar y eliminar (soft delete) comentarios.
 * </p>
 */
@RestController
@RequestMapping("api/v1/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;

    /**
     * Crea un nuevo comentario para un reporte.
     *
     * @param request Datos del comentario a crear.
     * @return Respuesta con el comentario creado.
     */
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@Valid @RequestBody CommentRequest request) {
        log.info("📝 Creando comentario para el reporte: {}", request.reportId());

        CommentResponse response = commentService.createComment(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        log.info("✅ Comentario creado con ID: {}", response.id());
        return ResponseEntity.created(location).body(response);
    }




    /**
     * Obtiene un comentario específico mediante su ID.
     *
     * @param commentId Identificador único del comentario.
     * @return Comentario encontrado, si existe.
     */
    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponse> getCommentById(@PathVariable String commentId) {
        log.info("🔍 Buscando comentario con ID: {}", commentId);
        CommentResponse response = commentService.getCommentById(commentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina lógicamente (soft delete) un comentario existente.
     * <p>
     * Solo el propietario del comentario o un administrador pueden realizar esta operación.
     * </p>
     *
     * @param commentId ID del comentario a eliminar.
     * @return Comentario actualizado con estado <code>ELIMINATED</code>.
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommentResponse> softDeleteComment(@PathVariable String commentId) {
        log.info("🗑️ Solicitud de soft delete para comentario con ID: {}", commentId);
        CommentResponse response = commentService.softDeleteComment(commentId);
        log.info("✅ Comentario con ID: {} marcado como ELIMINATED", commentId);
        return ResponseEntity.ok(response);
    }
}

