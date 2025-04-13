package co.edu.uniquindio.proyecto.exceptionhandler.notification;

import co.edu.uniquindio.proyecto.exception.notification.EmailNotificationException;
import co.edu.uniquindio.proyecto.exception.notification.WebSocketNotificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Controlador global de excepciones para errores relacionados con el envío de notificaciones.
 *
 * <p>Esta clase captura y gestiona excepciones personalizadas lanzadas durante el proceso de
 * notificación, tanto por WebSocket como por correo electrónico. Utiliza {@link ControllerAdvice}
 * para aplicarse de forma transversal a todos los controladores del sistema.</p>
 *
 * <p>En caso de una excepción, se registra el error en los logs y se devuelve una respuesta HTTP 500
 * con un mensaje genérico al cliente.</p>
 *
 * <p>Excepciones manejadas:</p>
 * <ul>
 *   <li>{@link WebSocketNotificationException} - Cuando falla el envío por WebSocket.</li>
 *   <li>{@link EmailNotificationException} - Cuando falla el envío por correo electrónico.</li>
 * </ul>
 */
@ControllerAdvice
@Slf4j
public class NotificationExceptionHandler {

    /**
     * Maneja errores relacionados con el envío de notificaciones por WebSocket.
     *
     * @param ex la excepción que fue lanzada
     * @return una respuesta HTTP 500 con un mensaje genérico de error
     */
    @ExceptionHandler(WebSocketNotificationException.class)
    public ResponseEntity<String> handleWebSocketError(WebSocketNotificationException ex) {
        log.warn("Excepción WebSocket: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body("Error enviando notificación WebSocket.");
    }

    /**
     * Maneja errores relacionados con el envío de notificaciones por correo electrónico.
     *
     * @param ex la excepción que fue lanzada
     * @return una respuesta HTTP 500 con un mensaje genérico de error
     */
    @ExceptionHandler(EmailNotificationException.class)
    public ResponseEntity<String> handleEmailError(EmailNotificationException ex) {
        log.warn("Excepción Email: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body("Error enviando notificación por correo.");
    }
}

