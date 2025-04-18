package co.edu.uniquindio.proyecto.dto.report;

import java.util.List;

public record PaginatedHistoryResponse(
        List<ReportStatusHistoryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
