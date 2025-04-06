package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.dto.comment.CommentPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.report.PaginatedReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportRequest;
import co.edu.uniquindio.proyecto.dto.report.ReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusUpdate;
import org.bson.types.ObjectId;

public interface ReportService {
    public ReportResponse createReport(ReportRequest request);
    public ReportResponse getReportById(String id);
    public PaginatedReportResponse getReportsNearLocation(double latitude, double longitude, Double radiusKm,
                                                          Integer page, Integer size
    );

    public void softDeleteReport(String reportId);

    public ReportResponse updateReport(ObjectId reportId, ReportRequest request);
    public void updateReportStatus(ObjectId reportId, ReportStatusUpdate dto);
    public void toggleReportVote(String reportId);
    public CommentPaginatedResponse getCommentsByReportId(String reportId, int page, int size);
}
