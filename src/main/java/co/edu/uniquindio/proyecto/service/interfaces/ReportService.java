package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.dto.report.ReportRequest;
import co.edu.uniquindio.proyecto.dto.report.ReportResponse;

public interface ReportService {
    public ReportResponse createReport(ReportRequest request);
}
