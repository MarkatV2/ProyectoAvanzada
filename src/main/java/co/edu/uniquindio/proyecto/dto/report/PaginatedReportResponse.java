package co.edu.uniquindio.proyecto.dto.report;

import java.util.List;

/**
 * Representa la respuesta paginada de los reportes.
 */
public record PaginatedReportResponse(
        List<ReportResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
