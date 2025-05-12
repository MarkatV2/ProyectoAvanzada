package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.notification.NotificationCreateDTO;
import co.edu.uniquindio.proyecto.entity.comment.Comment;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.exception.notification.EmailNotificationException;
import co.edu.uniquindio.proyecto.exception.notification.SseNotificationException;
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.service.interfaces.NotificationService;
import co.edu.uniquindio.proyecto.service.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio encargado de notificar al autor de un reporte cuando un nuevo comentario es creado por otro usuario.
 *
 * <p>Este servicio combina el uso de WebSockets y correo electrónico para enviar notificaciones.
 * Si el comentario es realizado por el mismo usuario que creó el reporte, no se realiza ninguna notificación.</p>
 *
 * <p>En caso de errores al enviar la notificación, se lanzan excepciones personalizadas:
 * <ul>
 *     <li>{@link EmailNotificationException} si ocurre un fallo al enviar el correo electrónico.</li>
 * </ul>
 * </p>
 *
 * <p>Dependencias:
 * <ul>
 *     <li>{@link NotificationService} para notificación en tiempo real.</li>
 *     <li>{@link EmailService} para envío de correos electrónicos.</li>
 *     <li>{@link NotificationMapper} para mapear el comentario a un DTO de notificación.</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentNotificationService {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final NotificationMapper notificationMapper;

    /**
     * Notifica al autor del reporte cuando otro usuario comenta.
     * La notificación se realiza mediante WebSocket y correo electrónico.
     * Si el autor del comentario es el mismo que el del reporte, no se realiza ninguna acción.
     *
     * @param comment        Comentario creado.
     * @param report         Reporte al que pertenece el comentario.
     * @param commenterName  Nombre del usuario que hizo el comentario.
     * @throws EmailNotificationException     si ocurre un error enviando la notificación por correo electrónico
     */
    public void notifyOwner(Comment comment, Report report, String commenterName) {
        if (report.getUserId().equals(comment.getUserId())) {
            log.debug("El autor del comentario es el mismo que el del reporte. No se envía notificación.");
            return;
        }

        NotificationCreateDTO dto = notificationMapper.buildFromComment(comment, report, commenterName);

        try {
            notificationService.notifyUser(dto);
        } catch (Exception e) {
            throw new SseNotificationException(
                    "Error al enviar notificación WebSocket al usuario con ID: " + report.getUserId(), e);
        }

        try {
            emailService.sendCommentEmail(
                    report.getUserEmail(),
                    commenterName,
                    report.getTitle(),
                    comment.getComment()
            );
        } catch (Exception e) {
            throw new EmailNotificationException(
                    "Error al enviar correo al usuario con email: " + report.getUserEmail(), e);
        }
    }
}

