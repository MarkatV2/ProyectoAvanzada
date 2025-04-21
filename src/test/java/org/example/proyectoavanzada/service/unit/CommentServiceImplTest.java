package org.example.proyectoavanzada.service.unit;

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
import co.edu.uniquindio.proyecto.service.implementations.CommentServiceImpl;
import co.edu.uniquindio.proyecto.service.interfaces.NotificationService;
import co.edu.uniquindio.proyecto.service.mapper.CommentMapper;
import co.edu.uniquindio.proyecto.util.SecurityUtils;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService commentNotificationService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CommentServiceImpl commentService;

    private List<Report> reports;

    private List<Comment> mockComments;

    private List<Comment> comments;

    private ObjectId reportId;

    @BeforeEach
    void setUp() {
        reportId = new ObjectId();
        comments = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> {
                    Comment c = new Comment();
                    c.setId(new ObjectId());
                    c.setReportId(reportId);
                    c.setUserId(new ObjectId());
                    c.setUserName("user" + i);
                    c.setComment("Comment " + i);
                    c.setCreatedAt(LocalDateTime.now());
                    c.setCommentStatus(null);
                    return c;
                })
                .toList();

        reports = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> {
                    Report r = new Report();
                    r.setId(new ObjectId());
                    r.setTitle("Reporte " + i);
                    r.setCreatedAt(LocalDateTime.now());
                    return r;
                })
                .toList();

        mockComments = List.of(
                createComment("user1", "Juan", "Comentario 1"),
                createComment("user2", "Ana", "Comentario 2"),
                createComment("user3", "Luis", "Comentario 3"),
                createComment("user4", "Sofía", "Comentario 4"),
                createComment("user5", "Carlos", "Comentario 5")
        );
    }

    private Report createReport(String title) {
        Report r = new Report();
        r.setId(new ObjectId());
        r.setTitle(title);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }


    private Comment createComment(String userId, String userName, String content) {
        Comment comment = new Comment();
        comment.setId(new ObjectId());
        comment.setUserId(new ObjectId());
        comment.setUserName(userName);
        comment.setComment(content);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setReportId(new ObjectId());
        return comment;
    }

    @Test
    @DisplayName("createComment - positivo: crea comentario y notifica al autor")
    void testCreateComment_Positive() {
        // Arrange
        Report targetReport = reports.get(1);
        String reportId = targetReport.getId().toHexString();
        // CommentRequest constructor: (comment, reportId)
        CommentRequest request = new CommentRequest("Texto de prueba", reportId);

        when(reportRepository.findById(new ObjectId(reportId)))
                .thenReturn(Optional.of(targetReport));
        when(securityUtils.getCurrentUserId()).thenReturn("userX");
        when(securityUtils.getCurrentUsername()).thenReturn("alice");

        Comment toSave = new Comment();
        toSave.setReportId(new ObjectId(reportId));
        toSave.setUserId(new ObjectId("507f1f77bcf86cd799439011"));
        toSave.setUserName("alice");
        toSave.setComment(request.comment());
        toSave.setCreatedAt(LocalDateTime.now());

        when(commentMapper.toEntity(request, "userX", "alice")).thenReturn(toSave);
// … dentro de tu @Test …
        Comment saved = new Comment();
        saved.setId(new ObjectId());
        saved.setReportId(toSave.getReportId());
        saved.setUserId(new ObjectId(toSave.getUserId()));            // copiar directamente
        saved.setUserName(toSave.getUserName());
        saved.setComment(toSave.getComment());
        saved.setCreatedAt(toSave.getCreatedAt());
        when(commentRepository.save(toSave)).thenReturn(saved);

        CommentResponse expected = new CommentResponse(
                saved.getId().toHexString(),
                saved.getUserName(),
                saved.getUserId(),            // convertir a String
                saved.getReportId().toHexString(),
                saved.getComment(),
                saved.getCreatedAt()
        );
        when(commentMapper.toResponse(saved)).thenReturn(expected);
// Act
        CommentResponse result = commentService.createComment(request);

// Assert
        assertNotNull(result);
        assertEquals(expected, result);

    }


    @Test
    @DisplayName("createComment - error: reporte no existe lanza ReportNotFoundException")
    void testCreateComment_ReportNotFound() {
        // Arrange
        String fakeReportId = new ObjectId().toHexString();
        // EL primer argumento es el texto del comentario, el segundo es el reportId
        CommentRequest request = new CommentRequest("Algún texto", fakeReportId);

        when(reportRepository.findById(new ObjectId(fakeReportId)))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ReportNotFoundException.class,
                () -> commentService.createComment(request));

        verify(reportRepository).findById(new ObjectId(fakeReportId));
        verifyNoInteractions(commentRepository, commentMapper, commentNotificationService);
    }


    @Test
    @DisplayName("createComment - positivo: crea comentario sin verificar notificación")
    void testCreateComment_NoNotificationCheck() {
        // Arrange
        Report targetReport = reports.get(0);
        String reportIdHex = targetReport.getId().toHexString();
        CommentRequest request = new CommentRequest("Texto sin notificación", reportIdHex);

        when(reportRepository.findById(new ObjectId(reportIdHex)))
                .thenReturn(Optional.of(targetReport));
        when(securityUtils.getCurrentUserId()).thenReturn("userZ");
        when(securityUtils.getCurrentUsername()).thenReturn("eve");

        Comment toSave = new Comment();
        toSave.setReportId(new ObjectId(reportIdHex));
        toSave.setUserId(new ObjectId("507f1f77bcf86cd799439012"));
        toSave.setUserName("eve");
        toSave.setComment(request.comment());
        toSave.setCreatedAt(LocalDateTime.now());
        when(commentMapper.toEntity(request, "userZ", "eve")).thenReturn(toSave);

        Comment saved = new Comment();
        saved.setId(new ObjectId());
        saved.setReportId(toSave.getReportId());
        saved.setUserId(new ObjectId(toSave.getUserId()));
        saved.setUserName(toSave.getUserName());
        saved.setComment(toSave.getComment());
        saved.setCreatedAt(toSave.getCreatedAt());
        when(commentRepository.save(toSave)).thenReturn(saved);

        CommentResponse expected = new CommentResponse(
                saved.getId().toHexString(),
                saved.getUserName(),
                saved.getUserId(),
                saved.getReportId().toHexString(),
                saved.getComment(),
                saved.getCreatedAt()
        );

        saved.setUserId(new ObjectId(toSave.getUserId()));

        when(commentMapper.toResponse(saved)).thenReturn(new CommentResponse(
                saved.getId().toHexString(),
                saved.getUserName(),
                saved.getUserId(),
                saved.getReportId().toHexString(),
                saved.getComment(),
                saved.getCreatedAt()
        ));
    }


    @Test
    @DisplayName("getCommentById - devuelve comentario correctamente")
    void testGetCommentById_Success() {
        // Arrange
        Comment comment = mockComments.get(3); // Sofía
        String id = comment.getId().toHexString();

        CommentResponse response = new CommentResponse(
                comment.getId().toHexString(),
                comment.getUserName(),
                comment.getUserId(),
                comment.getReportId().toHexString(),
                comment.getComment(),
                comment.getCreatedAt()
        );

        when(commentRepository.findById(new ObjectId(id))).thenReturn(Optional.of(comment));
        when(commentMapper.toResponse(comment)).thenReturn(response);

        // Act
        CommentResponse result = commentService.getCommentById(id);

        // Assert
        assertNotNull(result);
        assertEquals(comment.getUserName(), result.userName());
        assertEquals(comment.getComment(), result.comment());
        assertEquals(comment.getUserId(), result.userId());
        assertEquals(comment.getReportId().toHexString(), result.reportId());
        assertEquals(comment.getCreatedAt(), result.createdAt());
        verify(commentRepository).findById(new ObjectId(id));
        verify(commentMapper).toResponse(comment);
    }
    @Test
    @DisplayName("getCommentById - lanza excepción si no existe")
    void testGetCommentById_NotFound() {
        // Arrange
        String invalidId = new ObjectId().toHexString();
        when(commentRepository.findById(new ObjectId(invalidId))).thenReturn(Optional.empty());

        // Act & Assert
        CommentNotFoundException ex = assertThrows(CommentNotFoundException.class,
                () -> commentService.getCommentById(invalidId));

        assertEquals("Comentario no encontrado con ID: " + invalidId, ex.getMessage());
        verify(commentRepository).findById(new ObjectId(invalidId));
        verifyNoInteractions(commentMapper);
    }

    @Test
    @DisplayName("getCommentById - ID mal formado lanza IllegalArgumentException")
    void testGetCommentById_InvalidObjectId() {
        // Arrange
        String malformedId = "noEsUnObjectId";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> commentService.getCommentById(malformedId));
        verifyNoInteractions(commentRepository, commentMapper);
    }

    @Test
    @DisplayName("getCommentById - repositorio lanza excepción inesperada")
    void testGetCommentById_UnexpectedRepositoryError() {
        // Arrange
        Comment comment = mockComments.get(0);
        String id = comment.getId().toHexString();

        when(commentRepository.findById(new ObjectId(id)))
                .thenThrow(new RuntimeException("Error de conexión"));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> commentService.getCommentById(id));
        assertEquals("Error de conexión", ex.getMessage());
    }

    @Test
    @DisplayName("getCommentById - Mapper devuelve null")
    void testGetCommentById_MapperReturnsNull() {
        // Arrange
        Comment comment = mockComments.get(1);
        String id = comment.getId().toHexString();

        when(commentRepository.findById(new ObjectId(id))).thenReturn(Optional.of(comment));
        when(commentMapper.toResponse(comment)).thenReturn(null);

        // Act
        CommentResponse response = commentService.getCommentById(id);

        // Assert
        assertNull(response);
        verify(commentRepository).findById(new ObjectId(id));
        verify(commentMapper).toResponse(comment);
    }

    @Test
    @DisplayName("getCommentsByReportId - flujo positivo: retorna página de comentarios")
    void testGetCommentsByReportId_Positive() {
        // Arrange
        int page = 0, size = 2;
        var pageable = PageRequest.of(page, size);
        var pageImpl = new PageImpl<>(comments.subList(0, 2), pageable, comments.size());

        when(commentRepository.findByAllByReportId(reportId, pageable)).thenReturn(pageImpl);

        var responses = comments.subList(0, 2).stream()
                .map(c -> new CommentResponse(
                        c.getId().toHexString(),
                        c.getUserName(),
                        c.getUserId(),
                        c.getReportId().toHexString(),
                        c.getComment(),
                        c.getCreatedAt()
                ))
                .collect(Collectors.toList());
        when(commentMapper.toResponseList(pageImpl.getContent())).thenReturn(responses);

        // Act
        CommentPaginatedResponse result = commentService.getCommentsByReportId(reportId.toHexString(), page, size);

        // Assert
        assertNotNull(result);
        assertEquals(page, result.page());
        assertEquals(size, result.size());
        assertEquals(comments.size(), result.totalElements());
        assertEquals((comments.size() + size - 1) / size, result.totalPages());
        assertEquals(2, result.content().size());                     // usa content()
        assertEquals("Comment 1", result.content().get(0).comment());

        verify(commentRepository, times(1)).findByAllByReportId(reportId, pageable);
        verify(commentMapper, times(1)).toResponseList(pageImpl.getContent());
    }


    @Test
    @DisplayName("getCommentsByReportId - flujo negativo: ID inválido lanza IdInvalidException")
    void testGetCommentsByReportId_InvalidId() {
        // Act & Assert
        assertThrows(IdInvalidException.class,
                () -> commentService.getCommentsByReportId("invalid_id", 0, 5));
        verifyNoInteractions(commentRepository, commentMapper);
    }

    @Test
    @DisplayName("getCommentsByReportId - flujo negativo: sin comentarios lanza CommentNotFoundException")
    void testGetCommentsByReportId_NotFound() {
        // Arrange
        int page = 1, size = 5;
        var pageable = PageRequest.of(page, size);
        when(commentRepository.findByAllByReportId(reportId, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act & Assert
        String reportIdStr = reportId.toHexString();
        CommentNotFoundException ex = assertThrows(CommentNotFoundException.class,
                () -> commentService.getCommentsByReportId(reportIdStr, page, size));
        assertTrue(ex.getMessage().contains("No se encontraron comentarios para el reporte"));
        verify(commentRepository, times(1)).findByAllByReportId(reportId, pageable);
        verifyNoInteractions(commentMapper);
    }
    @Test
    @DisplayName("softDeleteComment - positivo: marca comentario como ELIMINATED y retorna DTO")
    void testSoftDeleteComment_Positive() {
        // Arrange
        Comment target = comments.get(2);
        String id = target.getId().toHexString();

        when(commentRepository.findById(new ObjectId(id)))
                .thenReturn(Optional.of(target));

        // Al llamarse setCommentStatus dentro del método, el mismo objeto 'target' cambia estado
        Comment updated = target;
        updated.setCommentStatus(CommentStatus.ELIMINATED);

        when(commentRepository.save(target)).thenReturn(updated);

        CommentResponse dto = new CommentResponse(
                updated.getId().toHexString(),
                updated.getUserName(),
                updated.getUserId(),
                updated.getReportId().toHexString(),
                updated.getComment(),
                updated.getCreatedAt()
        );
        when(commentMapper.toResponse(updated)).thenReturn(dto);

        // Act
        CommentResponse result = commentService.softDeleteComment(id);

        // Assert
        assertNotNull(result);
        verify(commentRepository, times(1)).findById(new ObjectId(id));
        verify(commentRepository, times(1)).save(target);
        verify(commentMapper, times(1)).toResponse(updated);
    }

    @Test
    @DisplayName("softDeleteComment - error: ID inválido lanza IdInvalidException")
    void testSoftDeleteComment_InvalidId() {
        // Act & Assert
        assertThrows(IdInvalidException.class, () -> commentService.softDeleteComment("invalid_id"));
        verifyNoInteractions(commentRepository, commentMapper);
    }

    @Test
    @DisplayName("softDeleteComment - error: comentario no existe lanza CommentNotFoundException")
    void testSoftDeleteComment_NotFound() {
        // Arrange
        String id = new ObjectId().toHexString();
        when(commentRepository.findById(new ObjectId(id)))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CommentNotFoundException.class, () -> commentService.softDeleteComment(id));
        verify(commentRepository, times(1)).findById(new ObjectId(id));
        verify(commentMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("softDeleteComment - error inesperado en save se propaga")
    void testSoftDeleteComment_SaveError() {
        // Arrange
        Comment target = comments.get(0);
        String id = target.getId().toHexString();
        when(commentRepository.findById(new ObjectId(id)))
                .thenReturn(Optional.of(target));
        // Simular falla al guardar
        when(commentRepository.save(target))
                .thenThrow(new RuntimeException("Error al guardar"));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> commentService.softDeleteComment(id));
        assertEquals("Error al guardar", ex.getMessage());
        verify(commentRepository).findById(new ObjectId(id));
        verify(commentRepository).save(target);
        verifyNoInteractions(commentMapper);
    }
}
