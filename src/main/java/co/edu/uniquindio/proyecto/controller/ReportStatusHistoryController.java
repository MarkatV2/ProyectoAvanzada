package co.edu.uniquindio.proyecto.controller;


import co.edu.uniquindio.proyecto.dto.report.ReportStatusHistoryPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusHistoryResponse;
import co.edu.uniquindio.proyecto.service.implementations.ReportStatusHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para consultar el historial de estados de reportes.
 */
@RestController
@RequestMapping("/api/v1/report-status-histories")
@RequiredArgsConstructor
@Slf4j
public class ReportStatusHistoryController {

    private final ReportStatusHistoryService historyService;

    /**
     * Endpoint para obtener un historial de estado por su ID.
     *
     * @param historyId ID del historial.
     * @return ResponseEntity con el ReportStatusHistoryResponse.
     */
    @GetMapping("/{historyId}")
    public ResponseEntity<ReportStatusHistoryResponse> getHistoryById(@PathVariable String historyId) {
        log.info("Recibida petición para obtener historial con ID: {}", historyId);
        ReportStatusHistoryResponse response = historyService.getHistoryById(historyId);
        return ResponseEntity.ok(response);
    }

    /*
     * Endpoint para obtener de forma paginada el historial de estados para un reporte.
     *
     * @param reportId ID del reporte.
     * @param page     Número de página (por defecto 0).
     * @param size     Tamaño de página (por defecto 10).
     * @return ResponseEntity con el ReportStatusHistoryPaginatedResponse.

    @GetMapping("/{reportId}")
    public ResponseEntity<ReportStatusHistoryPaginatedResponse> getHistoriesByReportId(
            @PathVariable String reportId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Recibida petición para obtener historial del reporte con ID: {} (página: {}, tamaño: {})", reportId, page, size);
        ReportStatusHistoryPaginatedResponse response = historyService.getHistoriesByReportId(reportId, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }  */
}
