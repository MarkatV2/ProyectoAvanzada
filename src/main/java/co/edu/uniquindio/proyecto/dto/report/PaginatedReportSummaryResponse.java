package co.edu.uniquindio.proyecto.dto.report;

import java.util.List;

public record PaginatedReportSummaryResponse(
        List<ReportSummaryDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}

