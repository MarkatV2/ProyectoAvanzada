package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.controller.NotificationSseController;
import co.edu.uniquindio.proyecto.dto.notification.NotificationCreateDTO;
import co.edu.uniquindio.proyecto.dto.notification.NotificationDTO;
import co.edu.uniquindio.proyecto.entity.notification.Notification;
import co.edu.uniquindio.proyecto.exception.notification.SseNotificationException;
import co.edu.uniquindio.proyecto.repository.NotificationRepository;
import co.edu.uniquindio.proyecto.service.interfaces.NotificationService;
import co.edu.uniquindio.proyecto.service.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio responsable de crear y enviar notificaciones a usuarios.
 * Encapsula la lógica de persistencia y envío a través de WebSocket
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSseController notificationSseController;
    private final NotificationMapper notificationMapper;

    /**
     * Crea y envía una notificación al usuario especificado.
     *
     * @param createDTO Objeto que contiene la información necesaria para crear la notificación.
     *                  Debe incluir el ID del usuario destinatario y los datos del evento.
     */
    @Override
    public void notifyUser(NotificationCreateDTO createDTO) {
        log.info("Creando notificación para el usuario con ID: {}", createDTO.userId());

        try {
            // Mapear DTO a entidad
            Notification notification = notificationMapper.fromCreateDTO(createDTO);

            // Guardar en base de datos
            Notification saved = notificationRepository.save(notification);
            log.debug("Notificación persistida con ID: {}", saved.getId());

            // Mapear a DTO para envío
            NotificationDTO dto = notificationMapper.toDTO(saved);

            // Intentar enviar por SSE
            boolean delivered = notificationSseController.sendNotification(createDTO.userId(), dto);

            if (delivered) {
                saved.setDelivered(true);
                notificationRepository.save(saved);
                log.info("Notificación entregada al usuario {}", createDTO.userId());
            } else {
                log.info("Notificación guardada como pendiente para el usuario {}", createDTO.userId());
            }

        } catch (Exception e) {
            log.error("Error al notificar al usuario {}: {}", createDTO.userId(), e.getMessage(), e);
            throw new SseNotificationException("Error al enviar la notificación SSE: ", e);
        }
    }

}
