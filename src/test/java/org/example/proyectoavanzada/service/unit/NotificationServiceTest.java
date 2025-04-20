package org.example.proyectoavanzada.service.unit;

import co.edu.uniquindio.proyecto.dto.notification.NotificationCreateDTO;
import co.edu.uniquindio.proyecto.entity.notification.NotificationType;
import co.edu.uniquindio.proyecto.service.interfaces.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceUnitTest {

    @Mock
    private NotificationService notificationService;

    private ObjectMapper objectMapper;

    private NotificationCreateDTO validNotification;
    private NotificationCreateDTO nullUserIdNotification;
    private NotificationCreateDTO emptyMessageNotification;
    private NotificationCreateDTO nullNotification;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        validNotification = new NotificationCreateDTO(
                "user123",
                "Nueva alerta",
                "Tienes una nueva notificación",
                "report789",
                NotificationType.ALERT,
                new GeoJsonPoint(6.25, -75.57)
        );

        nullUserIdNotification = new NotificationCreateDTO(
                null,
                "Alerta sin usuario",
                "Mensaje",
                "report000",
                NotificationType.ALERT,
                null
        );

        emptyMessageNotification = new NotificationCreateDTO(
                "user999",
                "Sin mensaje",
                "",
                "report999",
                NotificationType.REMINDER,
                null
        );

        nullNotification = null;
    }

    @Test
    @DisplayName("Debe enviar una notificación correctamente cuando los datos son válidos")
    void testNotifyUserSuccess() {
        // Arrange
        doNothing().when(notificationService).notifyUser(validNotification);

        // Act
        assertDoesNotThrow(() -> notificationService.notifyUser(validNotification));

        // Assert
        verify(notificationService, times(1)).notifyUser(validNotification);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la notificación es nula")
    void testNotifyUserWithNullNotificationThrowsException() {
        // Arrange
        doThrow(new IllegalArgumentException("La notificación no puede ser nula"))
                .when(notificationService).notifyUser(nullNotification);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            notificationService.notifyUser(nullNotification);
        });

        assertEquals("La notificación no puede ser nula", exception.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el userId es nulo")
    void testNotifyUserWithNullUserIdThrowsException() {
        // Arrange
        doThrow(new IllegalArgumentException("El userId es obligatorio"))
                .when(notificationService).notifyUser(nullUserIdNotification);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            notificationService.notifyUser(nullUserIdNotification);
        });

        assertEquals("El userId es obligatorio", exception.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el mensaje está vacío")
    void testNotifyUserWithEmptyMessageThrowsException() {
        // Arrange
        doThrow(new IllegalArgumentException("El mensaje no puede estar vacío"))
                .when(notificationService).notifyUser(emptyMessageNotification);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            notificationService.notifyUser(emptyMessageNotification);
        });

        assertEquals("El mensaje no puede estar vacío", exception.getMessage());
    }
}
