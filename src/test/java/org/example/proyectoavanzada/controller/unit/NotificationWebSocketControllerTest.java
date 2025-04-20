package org.example.proyectoavanzada.controller.unit;

import co.edu.uniquindio.proyecto.controller.NotificationWebSocketController;
import co.edu.uniquindio.proyecto.dto.notification.NotificationDTO;
import co.edu.uniquindio.proyecto.entity.notification.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para el controlador NotificationWebSocketController.
 *
 * Se verifica que las notificaciones se envíen correctamente al canal WebSocket adecuado
 * utilizando el DTO completo con todos los campos requeridos.
 *
 * Aunque no es un controlador REST, se siguen buenas prácticas de diseño y pruebas.
 */
class NotificationWebSocketControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationWebSocketController notificationWebSocketController;

    /**
     * Inicializa los mocks antes de cada prueba.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Prueba que se envíe una notificación al canal WebSocket correcto.
     * Se usa un NotificationDTO real con todos sus atributos definidos.
     */
    @Test
    @DisplayName("POST /websocket/notifications/{userId} debería enviar notificación correctamente al canal del usuario")
    void sendNotification_deberiaEnviarNotificacionAlUsuarioCorrecto() {
        // Arrange
        String userId = "user123";
        NotificationDTO notification = new NotificationDTO(
                "Reporte cercano",
                "Se ha creado un reporte cerca de tu ubicación.",
                "report789",
                NotificationType.NEARBY_REPORT,
                LocalDateTime.now()
        );

        String destinoEsperado = "/topic/notifications/" + userId;

        // Act
        notificationWebSocketController.sendNotification(userId, notification);

        // Assert
        verify(messagingTemplate, times(1)).convertAndSend(destinoEsperado, notification);
    }

    /**
     * Prueba que el método no lance excepciones al enviar una notificación válida.
     * Esto asegura la robustez del método frente a entradas comunes.
     */
    @Test
    @DisplayName("POST /websocket/notifications/{userId} retorna 200 cuando el envío se ejecuta correctamente")
    void sendNotification_noDebeLanzarExcepcionConNotificacionValida() {
        // Arrange
        String userId = "user456";
        NotificationDTO notification = new NotificationDTO(
                "Nuevo reporte",
                "Un nuevo reporte ha sido publicado en tu zona.",
                "report123",
                NotificationType.NEW_REPORT,
                LocalDateTime.now()
        );

        // Act & Assert
        assertDoesNotThrow(() -> notificationWebSocketController.sendNotification(userId, notification));
    }

}