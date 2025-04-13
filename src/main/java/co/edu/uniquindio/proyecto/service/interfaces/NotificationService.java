package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.dto.notification.NotificationCreateDTO;

/**
 * Servicio para enviar notificaciones a los usuarios.
 */
public interface NotificationService {

    /**
     * Envía una notificación al usuario especificado.
     *
     * @param createDTO información de la notificación a enviar.
     */
    void notifyUser(NotificationCreateDTO createDTO);
}
