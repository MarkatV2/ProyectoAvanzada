package co.edu.uniquindio.proyecto.exception.notification;

/**
 * Excepción lanzada cuando ocurre un error al enviar una notificación por correo electrónico.
 */
public class EmailNotificationException extends RuntimeException {
    public EmailNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
