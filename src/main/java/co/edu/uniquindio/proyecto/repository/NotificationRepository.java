package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.notification.Notification;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * Repositorio para la entidad {@link Notification}, maneja las operaciones de acceso a datos
 * para las notificaciones.
 */
public interface NotificationRepository extends MongoRepository<Notification, ObjectId> {
    @Query("{ 'userId': ?0, 'delivered': false }")
    List<Notification> findPendingByUserId(String userId);

    @Query(value = "{ 'userId': ?0, 'delivered': false }", count = true)
    long countPendingByUserId(String userId);
}

