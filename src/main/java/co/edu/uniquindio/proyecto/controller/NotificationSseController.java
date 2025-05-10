package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.notification.NotificationDTO;
import co.edu.uniquindio.proyecto.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationSseController {

    // Mapa concurrente para almacenar los emitters por ID de usuario
    private final Map<String, SseEmitter> clients = new ConcurrentHashMap<>();
    private final SecurityUtils securityUtils;

    /**
     * Endpoint para que el usuario autenticado se suscriba a las notificaciones SSE.
     *
     * @param userDetails Detalles del usuario autenticado.
     * @return SseEmitter para la conexión SSE.
     */
    /**
     * Endpoint para que el usuario autenticado se suscriba a las notificaciones SSE.
     *
     * @return SseEmitter para la conexión SSE.
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        String userId = securityUtils.getCurrentUserId();
        log.info("🟢 Usuario {} suscrito a notificaciones SSE", userId);

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        clients.put(userId, emitter);

        emitter.onCompletion(() -> {
            clients.remove(userId);
            log.info("🔴 Conexión SSE completada para usuario {}", userId);
        });

        emitter.onTimeout(() -> {
            clients.remove(userId);
            log.warn("⏰ Conexión SSE expirada para usuario {}", userId);
        });

        emitter.onError((e) -> {
            clients.remove(userId);
            log.error("❌ Error en conexión SSE para usuario {}", userId, e);
        });

        return emitter;
    }

    /**
     * Método para enviar una notificación SSE a un usuario específico.
     *
     * @param userId       ID del usuario receptor.
     * @param notification Objeto NotificationDTO con los datos de la notificación.
     */
    public void sendNotification(String userId, NotificationDTO notification) {
        SseEmitter emitter = clients.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("new-notification")
                        .data(notification));
                log.info("📨 Notificación enviada al usuario {}", userId);
            } catch (IOException e) {
                clients.remove(userId);
                log.error("❌ Error al enviar notificación al usuario {}", userId, e);
            }
        } else {
            log.warn("⚠️ No se encontró conexión SSE activa para el usuario {}", userId);
        }
    }

}
