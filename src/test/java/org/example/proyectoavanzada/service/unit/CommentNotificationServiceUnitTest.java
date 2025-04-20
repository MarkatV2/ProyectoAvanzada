package org.example.proyectoavanzada.service.unit;

import co.edu.uniquindio.proyecto.dto.notification.NotificationCreateDTO;
import co.edu.uniquindio.proyecto.entity.comment.Comment;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.exception.notification.EmailNotificationException;
import co.edu.uniquindio.proyecto.exception.notification.WebSocketNotificationException;
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.service.implementations.CommentNotificationService;
import co.edu.uniquindio.proyecto.service.interfaces.NotificationService;
import co.edu.uniquindio.proyecto.service.mapper.NotificationMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommentNotificationServiceUnitTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private CommentNotificationService commentNotificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void noDebeNotificarSiElAutorDelComentarioEsElMismoQueElDelReporte() {
        // Arrange
        Comment comment = new Comment();
        ObjectId userId = new ObjectId();

        comment.setUserId(userId);

        Report report = new Report();
        report.setUserId(userId);

        // Act
        commentNotificationService.notifyOwner(comment, report, "Juan");

        // Assert
        verify(notificationService, never()).notifyUser(any());
        verify(emailService, never()).sendCommentEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void debeNotificarPorWebSocketYCorreoSiSonUsuariosDiferentes() {
        // Arrange
        Comment comment = new Comment();
        comment.setUserId(new ObjectId());
        comment.setComment("Muy buen reporte");

        Report report = new Report();
        report.setUserId(new ObjectId());
        report.setUserEmail("test@example.com");
        report.setTitle("Título del reporte");

        NotificationCreateDTO dto = Mockito.mock(NotificationCreateDTO.class);

        when(notificationMapper.buildFromComment(comment, report, "Juan")).thenReturn(dto);

        // Act
        assertDoesNotThrow(() -> {
            commentNotificationService.notifyOwner(comment, report, "Juan");
        });

        // Assert
        verify(notificationService).notifyUser(dto);
        verify(emailService).sendCommentEmail(
                "test@example.com",
                "Juan",
                "Título del reporte",
                "Muy buen reporte"
        );
    }

    @Test
    void debeLanzarExcepcionWebSocketNotificationExceptionSiFallaNotificacionWebSocket() {
        // Arrange
        Comment comment = new Comment();
        comment.setUserId(new ObjectId());

        Report report = new Report();
        report.setUserId(new ObjectId());

        NotificationCreateDTO dto = Mockito.mock(NotificationCreateDTO.class);
        when(notificationMapper.buildFromComment(any(), any(), any())).thenReturn(dto);
        doThrow(new RuntimeException("WebSocket failure")).when(notificationService).notifyUser(dto);

        // Act & Assert
        assertThrows(WebSocketNotificationException.class, () -> {
            commentNotificationService.notifyOwner(comment, report, "Juan");
        });
    }

    @Test
    void debeLanzarExcepcionEmailNotificationExceptionSiFallaEnvioCorreo() {
        // Arrange
        Comment comment = new Comment();
        comment.setUserId(new ObjectId());
        comment.setComment("Comentario");

        Report report = new Report();
        report.setUserId(new ObjectId());
        report.setUserEmail("user@example.com");
        report.setTitle("Reporte X");

        NotificationCreateDTO dto = Mockito.mock(NotificationCreateDTO.class);
        when(notificationMapper.buildFromComment(any(), any(), any())).thenReturn(dto);
        doNothing().when(notificationService).notifyUser(dto);
        doThrow(new RuntimeException("Email fail")).when(emailService).sendCommentEmail(anyString(), anyString(), anyString(), anyString());

        // Act & Assert
        assertThrows(EmailNotificationException.class, () -> {
            commentNotificationService.notifyOwner(comment, report, "Carlos");
        });
    }
}
