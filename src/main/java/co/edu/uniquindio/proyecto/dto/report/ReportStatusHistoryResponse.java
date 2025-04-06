package co.edu.uniquindio.proyecto.dto.report;

import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import java.time.LocalDateTime;

/**
 * DTO para representar un historial de estado de un reporte.
 *
 * @param id              Identificador del historial.
 * @param reportId        Identificador del reporte.
 * @param userId          Identificador del usuario que realizó el cambio.
 * @param previousStatus  Estado anterior del reporte.
 * @param newStatus       Nuevo estado del reporte.
 * @param changedAt       Fecha y hora en que se realizó el cambio.
 */
public record ReportStatusHistoryResponse(
        String id,
        String reportId,
        String userId,
        ReportStatus previousStatus,
        ReportStatus newStatus,
        LocalDateTime changedAt
) {}
