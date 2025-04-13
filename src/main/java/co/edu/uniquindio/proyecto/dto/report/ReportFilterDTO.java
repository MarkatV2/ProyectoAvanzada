package co.edu.uniquindio.proyecto.dto.report;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para los filtros de búsqueda de reportes.
 */
public record ReportFilterDTO(
        LocalDateTime startDate,
        LocalDateTime endDate,
        List<String> categoryIds,
        GeoJsonPoint center,
        double radiusKm
) {}
