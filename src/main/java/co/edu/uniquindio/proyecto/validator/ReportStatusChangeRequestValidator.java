package co.edu.uniquindio.proyecto.validator;

import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReportStatusChangeRequestValidator {

    /**
     * Valida si el cambio de estado solicitado es permitido por el rol del usuario.
     *
     * @param report Reporte original.
     * @param newStatus Nuevo estado solicitado.
     * @param rejectionMessage Mensaje de rechazo (si aplica).
     * @param isAdmin Indica si el usuario es administrador.
     */
    public void validate(Report report, ReportStatus newStatus, String rejectionMessage, boolean isAdmin, String currentUserId) {
        switch (newStatus) {
            case VERIFIED, REJECTED -> {
                if (!isAdmin) {
                    log.error("Solo los administradores pueden cambiar el estado a VERIFIED o REJECTED.");
                    throw new SecurityException("Solo administradores pueden realizar esta acción.");
                }
                if (newStatus == ReportStatus.REJECTED && (rejectionMessage == null || rejectionMessage.isBlank())) {
                    log.error("Rechazo de reporte sin mensaje de justificación.");
                    throw new IllegalArgumentException("Debe proporcionar un mensaje de rechazo.");
                }
            }
            case RESOLVED -> {
                if (!report.getUserId().toString().equals(currentUserId) && !isAdmin) {
                    log.error("Solo el creador o un admin puede marcar un reporte como RESOLVED.");
                    throw new SecurityException("No autorizado para cambiar a RESOLVED.");
                }
            }
            default -> {
                log.error("Estado de reporte no permitido: {}", newStatus);
                throw new IllegalArgumentException("Transición de estado no válida.");
            }
        }
    }
}

