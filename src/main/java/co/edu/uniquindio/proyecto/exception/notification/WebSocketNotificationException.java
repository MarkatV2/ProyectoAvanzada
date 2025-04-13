package co.edu.uniquindio.proyecto.exception.notification;


/**
 * Excepción lanzada cuando ocurre un error al enviar una notificación WebSocket.
 */
public class WebSocketNotificationException extends RuntimeException {
    public WebSocketNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
