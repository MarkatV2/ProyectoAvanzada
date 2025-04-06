package co.edu.uniquindio.proyecto.dto.report;

import java.util.List;

/**
 * DTO para representar una respuesta paginada de historiales de estados de reportes.
 *
 * @param content       Lista de historiales.
 * @param page          Número de página actual.
 * @param size          Tamaño de página.
 * @param totalElements Total de elementos encontrados.
 * @param totalPages    Total de páginas.
 */
public record ReportStatusHistoryPaginatedResponse(
        List<ReportStatusHistoryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
