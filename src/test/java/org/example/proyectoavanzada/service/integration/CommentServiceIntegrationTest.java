package org.example.proyectoavanzada.service.integration;

import co.edu.uniquindio.proyecto.ProyectoApplication;
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
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.service.implementations.CommentNotificationService;
import co.edu.uniquindio.proyecto.service.interfaces.CommentService;
import co.edu.uniquindio.proyecto.service.interfaces.NotificationService;
import co.edu.uniquindio.proyecto.util.SecurityUtils;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ContextConfiguration(classes = ProyectoApplication.class)
public class CommentServiceIntegrationTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private CommentRepository commentRepository;
    @MockitoBean
    private CommentNotificationService commentNotificationService;

    @MockitoBean
    private SecurityUtils securityUtils;

    @MockitoBean
    private EmailService emailService;

    private List<Report> reports;

    private List<Comment> persisted;

    private ObjectId reportIdWithComments;
    private ObjectId reportIdNoComments;

    private List<Comment> persistedComments;

    @BeforeEach
    void setUp() {
        // Limpia colecciones
        commentRepository.deleteAll();
        reportRepository.deleteAll();

        // Crea 5 reportes
        reports = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> {
                    Report r = new Report();
                    r.setId(new ObjectId());
                    r.setTitle("Reporte " + i);
                    r.setCreatedAt(LocalDateTime.now());
                    return r;
                })
                .toList();
        reportRepository.saveAll(reports);

        // Crea y persiste 5 comentarios de usuarios distintos
        persisted = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> {
                    Comment c = new Comment();
                    c.setId(new ObjectId());
                    c.setReportId(new ObjectId());
                    c.setUserId(new ObjectId());
                    c.setUserName("user" + i);
                    c.setComment("Comentario " + i);
                    c.setCreatedAt(LocalDateTime.now());
                    c.setCommentStatus(CommentStatus.PUBLISHED);
                    return c;
                })
                .collect(Collectors.toList());
        persisted = commentRepository.saveAll(persisted);

        // crear dos reportes: uno con comentarios, otro sin
        Report reportWith = new Report();
        reportWith.setId(new ObjectId());
        reportWith.setTitle("ConComentarios");
        reportWith.setCreatedAt(LocalDateTime.now());

        Report reportWithout = new Report();
        reportWithout.setId(new ObjectId());
        reportWithout.setTitle("SinComentarios");
        reportWithout.setCreatedAt(LocalDateTime.now());

        reportRepository.saveAll(List.of(reportWith, reportWithout));

        reportIdWithComments = reportWith.getId();
        reportIdNoComments = reportWithout.getId();

        // crear 5 comentarios para reportWith
        List<Comment> comments = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> {
                    Comment c = new Comment();
                    c.setId(new ObjectId());
                    c.setReportId(reportIdWithComments);
                    c.setUserId(new ObjectId());
                    c.setUserName("user" + i);
                    c.setComment("Comentario " + i);
                    c.setCreatedAt(LocalDateTime.now());
                    c.setCommentStatus(CommentStatus.PUBLISHED);
                    return c;
                })
                .collect(Collectors.toList());
        commentRepository.saveAll(comments);

        persistedComments = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> {
                    Comment c = new Comment();
                    c.setId(new ObjectId());
                    c.setReportId(new ObjectId());
                    c.setUserId(new ObjectId());
                    c.setUserName("user" + i);
                    c.setComment("Comment " + i);
                    c.setCreatedAt(LocalDateTime.now());
                    c.setCommentStatus(CommentStatus.PUBLISHED);
                    return c;
                })
                .collect(Collectors.toList());
        persistedComments = commentRepository.saveAll(persistedComments);
    }

    @Test
    @DisplayName("createComment - positivo: crea y persiste comentario correctamente")
    void testCreateComment_Positive() {
        // Arrange
        Report target = reports.get(0);
        String reportId = target.getId().toHexString();
        CommentRequest request = new CommentRequest("Texto OK", reportId);

        when(securityUtils.getCurrentUserId()).thenReturn(new ObjectId().toHexString());
        when(securityUtils.getCurrentUsername()).thenReturn("alice");

        // Act
        CommentResponse response = commentService.createComment(request);

        // Assert DTO
        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals(reportId,         response.reportId());
        assertEquals("alice",          response.userName());
        assertEquals("Texto OK",       response.comment());

        // Verifica persistencia en BD
        Optional<Comment> saved = commentRepository.findById(new ObjectId(response.id()));
        assertTrue(saved.isPresent());
        assertEquals("Texto OK", saved.get().getComment());
        assertEquals("alice",    saved.get().getUserName());
    }


    @Test
    @DisplayName("createComment - error: reporte no existe lanza ReportNotFoundException")
    void testCreateComment_ReportNotFound() {
        // Arrange
        String fakeId = new ObjectId().toHexString();
        CommentRequest request = new CommentRequest("Texto", fakeId);

        when(reportRepository.findById(new ObjectId(fakeId)))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ReportNotFoundException.class, () -> commentService.createComment(request));

        // Verifica que no se intentó guardar ningún comentario
        verify(commentRepository, never()).save(any());
    }


    @Test
    @DisplayName("getCommentById - positivo: retorna comentario existente")
    void testGetCommentById_Success() {
        // Arrange
        Comment original = persisted.get(2);
        String id = original.getId().toHexString();

        when(commentRepository.findById(original.getId()))
                .thenReturn(Optional.of(original));

        // Act
        CommentResponse resp = commentService.getCommentById(id);

        // Assert
        assertNotNull(resp);
        assertEquals(id, resp.id());
        assertEquals(original.getUserName(), resp.userName());
        assertEquals(original.getUserId(), resp.userId());
        assertEquals(original.getReportId().toHexString(), resp.reportId());
        assertEquals(original.getComment(), resp.comment());
    }


    @Test
    @DisplayName("getCommentById - error: id inexistente lanza CommentNotFoundException")
    void testGetCommentById_NotFound() {
        // Arrange
        String fakeId = new ObjectId().toHexString();

        // Act & Assert
        assertThrows(CommentNotFoundException.class,
                () -> commentService.getCommentById(fakeId));
    }

    @Test
    @DisplayName("getCommentById - error: formato de ID inválido lanza IllegalArgumentException")
    void testGetCommentById_InvalidFormat() {
        // Arrange
        String badId = "123-invalid";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> commentService.getCommentById(badId));
    }

    @Test
    @DisplayName("getCommentsByReportId - positivo: paginación funciona")
    void testGetCommentsByReportId_Positive() {
        int page = 1, size = 2;
        // Act
        CommentPaginatedResponse resp = commentService.getCommentsByReportId(
                reportIdWithComments.toHexString(), page, size);
        // Assert
        assertNotNull(resp);
        assertEquals(page, resp.page());
        assertEquals(size, resp.size());
        assertEquals(5, resp.totalElements());
        assertEquals(3, resp.totalPages());
        // en página 1 (starting from 1) deben salir comentarios índices 2 y 3
        assertEquals(2, resp.content().size());
    }

    @Test
    @DisplayName("getCommentsByReportId - error: id inválido lanza IdInvalidException")
    void testGetCommentsByReportId_InvalidId() {
        String badId = "no-es-un-objectid";
        assertThrows(IdInvalidException.class,
                () -> commentService.getCommentsByReportId(badId, 1, 2));
    }

    @Test
    @DisplayName("getCommentsByReportId - ningún comentario retorna lista vacía")
    void testGetCommentsByReportId_Empty() {
        CommentPaginatedResponse resp = commentService.getCommentsByReportId(
                reportIdNoComments.toHexString(), 1, 5);
        assertNotNull(resp);
        assertEquals(1, resp.page());
        assertEquals(5, resp.size());
        assertEquals(0, resp.totalElements());
        assertEquals(0, resp.totalPages());
        assertTrue(resp.content().isEmpty());
    }

    @Test
    @DisplayName("softDeleteComment - positivo: marca comentario como ELIMINATED")
    void testSoftDeleteComment_Positive() {
        // Arrange
        Comment original = persistedComments.get(2);
        String id = original.getId().toHexString();

        // Act
        CommentResponse resp = commentService.softDeleteComment(id);

        // Assert response fields
        assertNotNull(resp);
        assertEquals(id, resp.id());
        assertEquals(original.getUserName(), resp.userName());
        assertEquals(original.getUserId(), resp.userId());
        assertEquals(original.getReportId().toHexString(), resp.reportId());
        assertEquals(original.getComment(), resp.comment());

        // Assert DB state
        Comment updated = commentRepository.findById(new ObjectId(id)).orElseThrow();
        assertEquals(CommentStatus.ELIMINATED, updated.getCommentStatus());
    }

    @Test
    @DisplayName("softDeleteComment - error: id inválido lanza IdInvalidException")
    void testSoftDeleteComment_InvalidId() {
        String badId = "not-a-valid-id";
        assertThrows(IdInvalidException.class,
                () -> commentService.softDeleteComment(badId));
    }

    @Test
    @DisplayName("softDeleteComment - error: comentario no existe lanza CommentNotFoundException")
    void testSoftDeleteComment_NotFound() {
        String missingId = new ObjectId().toHexString();
        assertThrows(CommentNotFoundException.class,
                () -> commentService.softDeleteComment(missingId));
    }

}

