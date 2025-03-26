package co.edu.uniquindio.proyecto.dto.report;

import java.util.List;

public record PaginatedReportResponse(
        List<ReportResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
