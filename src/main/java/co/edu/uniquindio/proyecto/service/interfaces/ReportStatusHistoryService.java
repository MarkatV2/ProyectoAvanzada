package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.dto.report.PaginatedHistoryResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusHistoryResponse;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportStatusHistoryService {

    public void createHistory(ObjectId reportId, ObjectId userId, ReportStatus previousStatus, ReportStatus newStatus);
    public ReportStatusHistoryResponse getHistoryById(String historyId);
    public PaginatedHistoryResponse getHistoryByReportId(String reportId, int page, int size);
    public PaginatedHistoryResponse getHistoryByUserId(String userId, int page, int size);
    public PaginatedHistoryResponse getHistoryByPreviousStatus(String reportId, ReportStatus previousStatus, int page, int size);
    public PaginatedHistoryResponse getHistoryByNewStatusAndDateRange(
            String reportId, ReportStatus newStatus, LocalDateTime startDate, LocalDateTime endDate, int page, int size);
    public long countHistoryByReportId(String reportId);
    public PaginatedHistoryResponse getHistoryByDateRange(String reportId, LocalDateTime startDate, LocalDateTime endDate, int page, int size);
}
