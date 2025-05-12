package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.notification.NotificationCreateDTO;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.service.interfaces.NotificationService;
import co.edu.uniquindio.proyecto.service.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio encargado de notificar a los usuarios que se encuentran dentro del radio de proximidad
 * definido con respecto a un nuevo reporte.
 *
 * Este servicio cumple con el principio de responsabilidad única (SRP), ya que se enfoca
 * únicamente en la lógica de notificación por cercanía.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NearbyNotificationService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    private final EmailService emailService;

    /**
     * Notifica a todos los usuarios que estén dentro del radio definido respecto al nuevo reporte.
     * Se excluye al creador del reporte y a usuarios sin ubicación registrada.
     *
     * @param report Reporte recién creado que se usará como referencia para calcular cercanía.
     */
    public void notifyUsersNearby(Report report) {
        log.info("Iniciando notificación por cercanía para el reporte con ID: {}", report.getId());

        List<User> allUsers = userRepository.findAll();
        log.debug("Total de usuarios en base de datos: {}", allUsers.size());

        List<User> nearbyUsers = allUsers.stream()
                .filter(user -> user.getLocation() != null)
                .filter(user -> !user.getId().toString().equals(report.getUserId()))
                .filter(user -> isWithinRadius(user.getLocation(), report.getLocation(), user.getNotificationRadiusKm()))
                .toList();

        log.info("Usuarios a notificar por cercanía: {}", nearbyUsers.size());


        for (User user : nearbyUsers) {
            try {
                // Notificación SSE
                NotificationCreateDTO dto = notificationMapper.buildFromReportForNearbyUser(report, user.getId().toString());
                notificationService.notifyUser(dto);
                log.debug("Notificación SSE enviada al usuario con ID: {}", user.getId());

                // Notificación por email
                emailService.sendNearbyReportEmail(
                        user.getEmail(),
                        user.getFullName(),
                        report.getTitle(),
                        report.getDescription()
                );

                log.debug("Email de notificación enviado a: {}", user.getEmail());

            } catch (Exception ex) {
                log.error("Error al notificar al usuario con ID: {}", user.getId(), ex);
            }
        }

        log.info("Proceso de notificación por cercanía finalizado para el reporte {}", report.getId());
    }


    /**
     * Determina si una ubicación se encuentra dentro del radio especificado.
     *
     * @param userLoc   Ubicación del usuario.
     * @param reportLoc Ubicación del reporte.
     * @param radiusKm  Radio en kilómetros dentro del cual se debe estar para considerar la cercanía.
     * @return true si está dentro del radio; false en caso contrario.
     */
    private boolean isWithinRadius(GeoJsonPoint userLoc, GeoJsonPoint reportLoc, double radiusKm) {
        double distanceKm = haversine(userLoc.getY(), userLoc.getX(), reportLoc.getY(), reportLoc.getX());
        return distanceKm <= radiusKm;
    }

    /**
     * Calcula la distancia entre dos coordenadas geográficas usando la fórmula de Haversine.
     *
     * @param lat1 Latitud del punto A.
     * @param lon1 Longitud del punto A.
     * @param lat2 Latitud del punto B.
     * @param lon2 Longitud del punto B.
     * @return Distancia en kilómetros entre los dos puntos.
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Radio de la Tierra en km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return R * 2 * Math.asin(Math.sqrt(a));
    }
}
