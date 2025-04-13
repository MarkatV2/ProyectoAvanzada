package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.notification.NotificationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Controlador encargado de enviar notificaciones en tiempo real a través de WebSocket.
 * <p>
 * Utiliza STOMP sobre WebSocket y publica mensajes en canales personalizados por usuario.
 * </p>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Envía una notificación a un usuario específico mediante WebSocket.
     * <p>
     * El mensaje se publica en el canal <code>/topic/notifications/{userId}</code>,
     * el cual debe estar suscrito en el cliente (Angular, etc.).
     * </p>
     *
     * @param userId       ID del usuario receptor.
     * @param notification Objeto {@link NotificationDTO} con los datos de la notificación.
     */
    public void sendNotification(String userId, NotificationDTO notification) {
        String destination = "/topic/notifications/" + userId;
        log.debug("📡 Enviando notificación al canal: {}", destination);
        messagingTemplate.convertAndSend(destination, notification);
    }
}