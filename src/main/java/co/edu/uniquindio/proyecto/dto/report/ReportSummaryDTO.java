package co.edu.uniquindio.proyecto.dto.report;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para obtener un resumen de un reporte con detalles básicos.
 */
public record ReportSummaryDTO(
        String title,
        String description,
        List<String> categoryNames,
        String status,
        LocalDateTime createdAt,
        double latitude,
        double longitude
) {}
