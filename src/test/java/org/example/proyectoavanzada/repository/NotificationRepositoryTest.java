package org.example.proyectoavanzada.repository;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.entity.notification.Notification;
import co.edu.uniquindio.proyecto.repository.NotificationRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@ContextConfiguration(classes = ProyectoApplication.class)
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;


    @BeforeEach
    void setUp() {
        // Limpiar la base de datos antes de cada prueba
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("Guardar una notificación correctamente")
    void testSaveNotification() {
        // Arrange: Se crea una notificación de prueba
        Notification notification = new Notification();
        notification.setTitle("Notificación de prueba");
        notification.setMessage("Este es un mensaje de prueba");
        notification.setRead(false);

        // Act: Guardar la notificación
        Notification savedNotification = notificationRepository.save(notification);

        // Assert: Verificar que la notificación se guardó correctamente
        assertNotNull(savedNotification.getId(), "La notificación debe tener un ID asignado");
        assertEquals("Notificación de prueba", savedNotification.getTitle(), "El título de la notificación debe coincidir");
        assertEquals("Este es un mensaje de prueba", savedNotification.getMessage(), "El mensaje de la notificación debe coincidir");
        assertFalse(savedNotification.isRead(), "El estado de lectura debe ser 'false' inicialmente");
    }

    @Test
    @DisplayName("Buscar notificación por ID")
    void testFindNotificationById() {
        // Arrange: Crear y guardar una notificación
        Notification notification = new Notification();
        notification.setTitle("Notificación para búsqueda");
        notification.setMessage("Mensaje para prueba de búsqueda");
        notification.setRead(true);
        Notification savedNotification = notificationRepository.save(notification);

        // Act: Buscar la notificación por ID
        Notification foundNotification = notificationRepository.findById(savedNotification.getId()).orElse(null);

        // Assert: Verificar que la notificación encontrada no sea nula y los datos sean correctos
        assertNotNull(foundNotification, "La notificación encontrada no debe ser nula");
        assertEquals(savedNotification.getId(), foundNotification.getId(), "El ID de la notificación encontrada debe coincidir");
        assertEquals("Notificación para búsqueda", foundNotification.getTitle(), "El título de la notificación debe coincidir");
        assertEquals("Mensaje para prueba de búsqueda", foundNotification.getMessage(), "El mensaje de la notificación debe coincidir");
        assertTrue(foundNotification.isRead(), "El estado de lectura debe ser 'true'");
    }

    @Test
    @DisplayName("Eliminar una notificación")
    void testDeleteNotification() {
        // Arrange: Crear y guardar una notificación
        Notification notification = new Notification();
        notification.setTitle("Notificación para eliminar");
        notification.setMessage("Mensaje de prueba para eliminar");
        notification.setRead(false);
        Notification savedNotification = notificationRepository.save(notification);

        // Act: Eliminar la notificación
        notificationRepository.delete(savedNotification);

        // Assert: Verificar que la notificación haya sido eliminada
        assertFalse(notificationRepository.existsById(savedNotification.getId()), "La notificación debería haber sido eliminada");
    }

    @Test
    @DisplayName("Buscar notificación por ID no existente")
    void testFindNotificationByIdNotFound() {
        // Act: Intentar buscar una notificación con un ID que no existe
        Notification foundNotification = notificationRepository.findById(new ObjectId()).orElse(null);

        // Assert: Verificar que no se encuentre ninguna notificación
        assertNull(foundNotification, "No debe existir una notificación con un ID no registrado");
    }

    @Test
    @DisplayName("Guardar notificación con campos nulos")
    void testSaveNotificationWithNullFields() {
        // Arrange: Crear una notificación con campos nulos
        Notification notification = new Notification();
        notification.setTitle(null);
        notification.setMessage(null);
        notification.setRead(false);

        // Act: Intentar guardar la notificación
        Notification savedNotification = notificationRepository.save(notification);

        // Assert: Verificar que la notificación se guarda con valores nulos
        assertNull(savedNotification.getTitle(), "El título debe ser nulo");
        assertNull(savedNotification.getMessage(), "El mensaje debe ser nulo");
        assertFalse(savedNotification.isRead(), "El estado de lectura debe ser 'false' inicialmente");
    }

    @Test
    @DisplayName("Buscar todas las notificaciones")
    void testFindAllNotifications() {
        // Arrange: Crear y guardar varias notificaciones
        Notification notification1 = new Notification();
        notification1.setTitle("Notificación 1");
        notification1.setMessage("Mensaje de notificación 1");
        notification1.setRead(false);

        Notification notification2 = new Notification();
        notification2.setTitle("Notificación 2");
        notification2.setMessage("Mensaje de notificación 2");
        notification2.setRead(true);

        notificationRepository.save(notification1);
        notificationRepository.save(notification2);

        // Act: Obtener todas las notificaciones
        Iterable<Notification> notifications = notificationRepository.findAll();

        // Assert: Verificar que se hayan recuperado dos notificaciones
        assertTrue(notifications.spliterator().getExactSizeIfKnown() > 0, "Debe haber al menos una notificación guardada");
    }

    @Test
    @DisplayName("Verificar existencia de notificación")
    void testExistsById() {
        // Arrange: Crear y guardar una notificación
        Notification notification = new Notification();
        notification.setTitle("Notificación para existencia");
        notification.setMessage("Mensaje para verificar existencia");
        notification.setRead(false);
        Notification savedNotification = notificationRepository.save(notification);

        // Act: Verificar si existe la notificación
        boolean exists = notificationRepository.existsById(savedNotification.getId());

        // Assert: Verificar que la notificación existe
        assertTrue(exists, "La notificación debe existir en la base de datos");
    }

}
