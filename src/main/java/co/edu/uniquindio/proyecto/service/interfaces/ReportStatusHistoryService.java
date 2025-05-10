package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.dto.report.PaginatedHistoryResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusHistoryResponse;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import org.bson.types.ObjectId;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;


@PreAuthorize("hasRole('ADMIN')") // Aplica a todos los métodos
public interface ReportStatusHistoryService {

    void createHistory(ObjectId reportId, ObjectId userId, ReportStatus previousStatus, ReportStatus newStatus);

    PaginatedHistoryResponse getAllHistories(int page, int size);

    ReportStatusHistoryResponse getHistoryById(String historyId);
    PaginatedHistoryResponse getHistoryByReportId(String reportId, int page, int size);
    PaginatedHistoryResponse getHistoryByUserId(String userId, int page, int size);
    PaginatedHistoryResponse getHistoryByPreviousStatus(String reportId, ReportStatus previousStatus, int page, int size);
    PaginatedHistoryResponse getHistoryByNewStatusAndDateRange(
            String reportId, ReportStatus newStatus, LocalDateTime startDate, LocalDateTime endDate, int page, int size);
    long countHistoryByReportId(String reportId);
    PaginatedHistoryResponse getHistoryByDateRange(String reportId, LocalDateTime startDate, LocalDateTime endDate, int page, int size);
}
