package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.notification.NotificationDTO;
import co.edu.uniquindio.proyecto.entity.notification.Notification;
import co.edu.uniquindio.proyecto.repository.NotificationRepository;
import co.edu.uniquindio.proyecto.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationSseController {

    private final Map<String, SseEmitter> clients = new ConcurrentHashMap<>();
    private final SecurityUtils securityUtils;
    private final NotificationRepository notificationRepository;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter subscribe() {
    String userId = securityUtils.getCurrentUserId();
    log.info("🟢 Usuario {} suscrito a notificaciones SSE", userId);

    // Timeout a 0 = conexión persistente hasta que el cliente cierre
    SseEmitter emitter = new SseEmitter(0L);

    // 1) registramos el cliente para notificaciones futuras
    clients.put(userId, emitter);

    // 2) configuramos los callbacks (onCompletion, onError, onTimeout)
    configureEmitter(userId, emitter);

    // 3) enviamos las notificaciones pendientes
    sendPendingNotifications(userId, emitter);

    return emitter;
}

private void sendPendingNotifications(String userId, SseEmitter emitter) {
    List<Notification> pendientes = notificationRepository.findPendingByUserId(userId);
    log.info("🔔 Enviando {} notificaciones pendientes a {}", pendientes.size(), userId);

    for (Notification noti : pendientes) {
        try {
            NotificationDTO dto = convertToDto(noti);
            emitter.send(SseEmitter.event()
                    .name("new-notification")
                    .data(dto, MediaType.APPLICATION_JSON));

            // Marcar como entregada y guardar
            noti.setDelivered(true);
            notificationRepository.save(noti);

            log.info("✅ Pendiente {} enviada y marcada como entregada", noti.getId());
        } catch (IOException e) {
            log.error("❌ Error al enviar notificación {} a {}", noti.getId(), userId, e);
        }
    }
}


    private void configureEmitter(String userId, SseEmitter emitter) {
        emitter.onCompletion(() -> {
            clients.remove(userId);
            log.info("🔴 Conexión SSE completada para usuario {}", userId);
        });

        emitter.onTimeout(() -> {
            clients.remove(userId);
            log.warn("⏰ Conexión SSE expirada para usuario {}", userId);
        });

        emitter.onError(e -> {
            clients.remove(userId);
            log.error("❌ Error en conexión SSE para usuario {}", userId, e);
        });
    }

    public boolean sendNotification(String userId, NotificationDTO notification) {
        SseEmitter emitter = clients.get(userId);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("new-notification")
                        .data(notification, MediaType.APPLICATION_JSON));
                log.info("📨 Notificación enviada al usuario {}", userId);
                return true;
            } catch (IOException e) {
                clients.remove(userId);
                log.error("❌ Error al enviar notificación al usuario {}", userId, e);
                return false;
            }
        }

        return false;
    }

    private NotificationDTO convertToDto(Notification notification) {
        return new NotificationDTO(
                notification.getId().toHexString(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReportId(),
                notification.getType(),
                notification.getCreatedAt()
        );
    }
}
