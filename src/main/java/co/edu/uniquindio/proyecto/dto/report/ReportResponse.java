package co.edu.uniquindio.proyecto.dto.report;

import co.edu.uniquindio.proyecto.entity.category.CategoryRef;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de un reporte, contiene los detalles de un reporte específico.
 */
public record ReportResponse(
        String id,
        String title,
        String description,
        List<CategoryRef> categoryList,
        double latitude,
        double longitude,
        String reportStatus,
        LocalDateTime createdAt,
        int importantVotes,
        String userId
) {}
