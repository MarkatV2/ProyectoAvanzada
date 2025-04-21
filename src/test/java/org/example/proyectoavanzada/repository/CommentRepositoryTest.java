package org.example.proyectoavanzada.repository;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.entity.comment.Comment;
import co.edu.uniquindio.proyecto.entity.comment.CommentStatus;
import co.edu.uniquindio.proyecto.repository.CommentRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@ContextConfiguration(classes = ProyectoApplication.class)
public class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    // Lista de comentarios para el dataset
    private List<Comment> comments;

    // Se usará este ObjectId para agrupar algunos comentarios de un reporte
    private ObjectId reportId1;
    // Y otro para comentarios asociados a otro reporte
    private ObjectId reportId2;

    @BeforeEach
    public void setUp() {
        // Limpiar la base de datos para pruebas repetibles
        mongoTemplate.getDb().listCollectionNames().forEach(mongoTemplate::dropCollection);

        comments = new ArrayList<>();

        // Crear identificadores para los reportes
        reportId1 = new ObjectId();
        reportId2 = new ObjectId();

        /*
         * Crear al menos 5 comentarios (de 5 usuarios distintos, simulados por el campo userId)
         * Se agregarán:
         * - 3 comentarios para reportId1: 2 publicados y 1 en otro estado (por ejemplo, DRAFT)
         * - 2 comentarios para reportId2, ambos publicados.
         */
        // Comentarios para reportId1
        comments.add(createComment(new ObjectId(), "Usuario1", reportId1, "Comentario 1", CommentStatus.PUBLISHED));
        comments.add(createComment(new ObjectId(), "Usuario2", reportId1, "Comentario 2", CommentStatus.PUBLISHED));
        comments.add(createComment(new ObjectId(), "Usuario3", reportId1, "Comentario 3", CommentStatus.ELIMINATED));

        // Comentarios para reportId2
        comments.add(createComment(new ObjectId(), "Usuario4", reportId2, "Comentario 4", CommentStatus.PUBLISHED));
        comments.add(createComment(new ObjectId(), "Usuario5", reportId2, "Comentario 5", CommentStatus.PUBLISHED));

        // Guardar todos los comentarios en la base de datos
        commentRepository.saveAll(comments);
    }

    /**
     * Método auxiliar para crear instancias de Comment.
     */
    private Comment createComment(ObjectId userId, String userName, ObjectId reportId, String commentText, CommentStatus status) {
        Comment comment = new Comment();
        // Se asume que el id se autogenera (ObjectId)
        comment.setUserId(userId);
        comment.setUserName(userName);
        comment.setReportId(reportId);
        comment.setComment(commentText);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setCommentStatus(status);
        return comment;
    }

    @Test
    @DisplayName("Test findByReportId: Debe retornar solo los comentarios publicados para un reporte específico")
    public void testFindAllByReportId() {
        // Se consulta los comentarios del reporte reportId1 usando paginación (página 0, tamaño 10)
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Comment> page = commentRepository.findByAllByReportId(reportId1, pageRequest);

        /*
         * Aunque en el dataset hay 3 comentarios para reportId1, solo 2 tienen el estado PUBLISHED.
         * Se espera que el Page tenga 2 elementos.
         */
        assertNotNull(page, "El resultado no debe ser nulo.");
        assertEquals(2, page.getTotalElements(), "Se esperaban 2 comentarios publicados para reportId1.");
        // Se valida que cada comentario recuperado tenga el estado PUBLISHED
        page.getContent().forEach(c -> assertEquals(CommentStatus.PUBLISHED, c.getCommentStatus()));
    }

    @Test
    @DisplayName("Test findById: Retorna un comentario publicado dado su ID")
    public void testFindByIdPublished() {
        // Seleccionamos uno de los comentarios publicados (por ejemplo, el primero de reportId2)
        Comment comentarioEsperado = comments.stream()
                .filter(c -> c.getReportId().equals(reportId2) && c.getCommentStatus() == CommentStatus.PUBLISHED)
                .findFirst().orElseThrow();

        // Se consulta el comentario por su id
        Optional<Comment> resultado = commentRepository.findById(comentarioEsperado.getId());

        // Se espera que se encuentre y que tenga el estado PUBLISHED
        assertTrue(resultado.isPresent(), "El comentario debería existir.");
        assertEquals(CommentStatus.PUBLISHED, resultado.get().getCommentStatus(), "El comentario debe estar publicado.");
    }

    @Test
    @DisplayName("Test findById: No debe retornar comentarios si no están publicados")
    public void testFindByIdNonPublished() {
        // Seleccionamos el comentario cuyo status no es PUBLISHED (por ejemplo, DRAFT)
        Comment comentarioNoPublicado = comments.stream()
                .filter(c -> c.getReportId().equals(reportId1) && c.getCommentStatus() != CommentStatus.PUBLISHED)
                .findFirst().orElseThrow();

        // Se consulta el comentario por su id
        Optional<Comment> resultado = commentRepository.findById(comentarioNoPublicado.getId());

        // Se espera que no se retorne el comentario, ya que el query filtra por PUBLISHED
        assertFalse(resultado.isPresent(), "No se debe retornar un comentario que no esté publicado.");
    }
}
