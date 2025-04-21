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
import co.edu.uniquindio.proyecto.service.implementations.CommentNotificationService;
import co.edu.uniquindio.proyecto.service.implementations.CommentServiceImpl;
import co.edu.uniquindio.proyecto.service.interfaces.NotificationService;
import co.edu.uniquindio.proyecto.service.mapper.CommentMapper;
import co.edu.uniquindio.proyecto.util.SecurityUtils;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceUnitTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private EmailService emailService;

    @Mock
    private CommentNotificationService commentNotificationService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CommentServiceImpl commentService;

    private List<Report> reports;

    private List<Comment> mockComments;

    private List<Comment> comments;

    private ObjectId reportId;//
    private CommentRequest request;
    private Report reportEntity;
    private Comment commentEntity;
    private Comment savedComment;
    private CommentResponse expectedResponse;

    @BeforeEach
    void setUp() {
        // Creamos IDs de ejemplo
        reportId = new ObjectId();
        String userId    = new ObjectId().toHexString();
        String userName  = "juan.perez";
        String commentId = new ObjectId().toHexString();

        // 1) El request que llega al método
        request = new CommentRequest(
                "Este es un comentario de prueba",
                reportId.toHexString()
        );

        // 2) El Report que devuelve el repo
        reportEntity = new Report();
        reportEntity.setId(new ObjectId(reportId.toHexString()));
        reportEntity.setTitle("Incidente X");
        // … otros campos del report si los necesitas

        // 3) La entidad Comment que devuelve el mapper antes de guardar
        commentEntity = new Comment();
        commentEntity.setId(new ObjectId(commentId)); // aunque normalmente se genera al guardar
        commentEntity.setReportId(new ObjectId(reportId.toHexString()));
        commentEntity.setComment(request.comment());
        commentEntity.setUserId(new ObjectId(userId));
        commentEntity.setUserName(userName);
        commentEntity.setCreatedAt(LocalDateTime.now());

        // 4) El Comment ya guardado (simulamos que el repo lo devuelve igual que mapper)
        savedComment = commentEntity;

        // 5) La respuesta esperada tras mapear el savedComment
        expectedResponse = new CommentResponse(
                commentId,
                userName,
                userId,
                reportId.toHexString(),
                request.comment(),
                savedComment.getCreatedAt()
        );
        String finalReportId = reportId.toHexString();
        comments = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> {
                    Comment c = new Comment();
                    c.setId(new ObjectId());
                    c.setReportId(new ObjectId(finalReportId));
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
    @DisplayName("createComment - flujo positivo: crea comentario y notifica al dueño del reporte")
    void testCreateComment_PositiveFlow() {
        // Arrange
        Report mockReport = new Report();
        ObjectId reportId = new ObjectId("507f1f77bcf86cd799439011");
        mockReport.setId(reportId);

        String reportIdHex = reportId.toHexString();
        String userId = "507f1f77bcf86cd799439012";
        String username = "eve";

        CommentRequest request = new CommentRequest("Comentario de prueba", reportIdHex);

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(mockReport));
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.getCurrentUsername()).thenReturn(username);

        Comment toSave = new Comment();
        toSave.setReportId(reportId);
        toSave.setUserId(new ObjectId(userId));
        toSave.setUserName(username);
        toSave.setComment(request.comment());
        toSave.setCreatedAt(LocalDateTime.now());

        when(commentMapper.toEntity(request, userId, username)).thenReturn(toSave);

        Comment savedComment = new Comment();
        ObjectId savedId = new ObjectId("507f1f77bcf86cd799439013");
        savedComment.setId(savedId);
        savedComment.setReportId(toSave.getReportId());
        savedComment.setUserId(new ObjectId(toSave.getUserId()));
        savedComment.setUserName(toSave.getUserName());
        savedComment.setComment(toSave.getComment());
        savedComment.setCreatedAt(toSave.getCreatedAt());

        when(commentRepository.save(toSave)).thenReturn(savedComment);

        CommentResponse expectedResponse = new CommentResponse(
                savedId.toHexString(),
                username,
                userId,
                reportIdHex,
                request.comment(),
                savedComment.getCreatedAt()
        );

        when(commentMapper.toResponse(savedComment)).thenReturn(expectedResponse);

        // Act
        CommentResponse actualResponse = commentService.createComment(request);

        // Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse.id(), actualResponse.id());
        assertEquals(expectedResponse.userName(), actualResponse.userName());
        assertEquals(expectedResponse.userId(), actualResponse.userId());
        assertEquals(expectedResponse.reportId(), actualResponse.reportId());
        assertEquals(expectedResponse.comment(), actualResponse.comment());
        assertEquals(expectedResponse.createdAt(), actualResponse.createdAt());

        verify(reportRepository).findById(reportId);
        verify(securityUtils).getCurrentUserId();
        verify(securityUtils).getCurrentUsername();
        verify(commentMapper).toEntity(request, userId, username);
        verify(commentRepository).save(toSave);
        verify(commentMapper).toResponse(savedComment);
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
