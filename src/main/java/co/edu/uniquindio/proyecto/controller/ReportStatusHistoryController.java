package co.edu.uniquindio.proyecto.controller;


import co.edu.uniquindio.proyecto.dto.report.ReportStatusHistoryPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusHistoryResponse;
import co.edu.uniquindio.proyecto.service.implementations.ReportStatusHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportStatusHistoryResponse> getHistoryById(@PathVariable String historyId) {
        log.info("Recibida petición para obtener historial con ID: {}", historyId);
        ReportStatusHistoryResponse response = historyService.getHistoryById(historyId);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene el historial de cambios de estado de un reporte dentro de un rango de fechas, con paginación.
     *
     * @param reportId  El ID del reporte.
     * @param startDate Fecha de inicio (formato ISO).
     * @param endDate   Fecha de fin (formato ISO).
     * @param page      Número de página (comienza en 0).
     * @param size      Tamaño de página.
     * @return Página de registros filtrados según el rango de fechas.
     */
    @GetMapping("/{reportId}/date-range")
    public ResponseEntity<List<ReportStatusHistoryResponse>> getHistoryByDateRange(
            @PathVariable("reportId") String reportId,
            @RequestParam("startDate") LocalDateTime startDate,
            @RequestParam("endDate") LocalDateTime endDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {


        log.info("Solicitando historial del reporte {} desde {} hasta {} (página {}, tamaño {})",
                reportId, startDate, endDate, page, size);

        List<ReportStatusHistoryResponse> historyPage = historyService.getHistoryByDateRange(reportId, startDate, endDate, page, size);
        return ResponseEntity.ok(historyPage);
    }
}
