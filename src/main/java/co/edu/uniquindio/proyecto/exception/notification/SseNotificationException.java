package co.edu.uniquindio.proyecto.exception.notification;


public class SseNotificationException extends RuntimeException {
    public SseNotificationException(String message) {
        super(message);
    }

    public SseNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}