package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.notification.Notification;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repositorio para la entidad {@link Notification}, maneja las operaciones de acceso a datos
 * para las notificaciones.
 */
public interface NotificationRepository extends MongoRepository<Notification, ObjectId> {
    // Sin métodos adicionales, este repositorio hereda operaciones CRUD básicas
}

