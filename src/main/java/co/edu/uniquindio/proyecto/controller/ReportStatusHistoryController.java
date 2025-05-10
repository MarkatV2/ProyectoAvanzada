package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.report.PaginatedHistoryResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusHistoryResponse;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.exception.report.HistoryNotFoundException;
import co.edu.uniquindio.proyecto.service.implementations.ReportStatusHistoryServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

/**
 * Controlador para consultar el historial de estados de reportes.
 */
@RestController
@RequestMapping("/api/v1/report-status-histories")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')") // Aplica a todos los métodos
public class ReportStatusHistoryController {

    private final ReportStatusHistoryServiceImpl historyService;

    /**
     * Recupera un historial de cambios de estado específico a partir de su identificador único.
     *
     * @param historyId ID del historial a consultar.
     * @return {@link ReportStatusHistoryResponse} con los datos del historial correspondiente.
     * @throws HistoryNotFoundException si no se encuentra un historial con el ID proporcionado.
     */
    @GetMapping("/{historyId}")
    public ResponseEntity<ReportStatusHistoryResponse> getHistoryById(@PathVariable String historyId) {
        log.info("📄 Obteniendo historial con ID: {}", historyId);
        return ResponseEntity.ok(historyService.getHistoryById(historyId));
    }

    @GetMapping()
    public ResponseEntity<PaginatedHistoryResponse> getAllHistories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("📄 Todos los Historiales (página {}, tamaño {})" , page, size);
        return ResponseEntity.ok(historyService.getAllHistories(page, size));
    }


    /**
     * Retorna el historial completo de cambios de estado asociados a un reporte específico,
     * con soporte de paginación.
     *
     * @param reportId ID del reporte cuyo historial se desea obtener.
     * @param page     Número de página (comienza en 1).
     * @param size     Tamaño de la página de resultados.
     * @return Lista paginada de {@link ReportStatusHistoryResponse}.
     */
    @GetMapping("/by-report")
    public ResponseEntity<PaginatedHistoryResponse> getByReportId(
            @RequestParam String reportId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("📄 Historial completo del reporte {} (página {}, tamaño {})", reportId, page, size);
        return ResponseEntity.ok(historyService.getHistoryByReportId(reportId, page, size));
    }


    /**
     * Retorna el historial de cambios de estado de un reporte que ocurrieron dentro de un
     * rango de fechas determinado, con paginación.
     *
     * @param reportId  ID del reporte.
     * @param startDate Fecha de inicio del rango (formato ISO 8601).
     * @param endDate   Fecha de fin del rango (formato ISO 8601).
     * @param page      Número de página (comienza en 1).
     * @param size      Tamaño de la página de resultados.
     * @return Lista de {@link ReportStatusHistoryResponse} correspondiente al filtro aplicado.
     */
    @GetMapping("/by-report/date-range")
    public ResponseEntity<PaginatedHistoryResponse> getByDateRange(
            @RequestParam String reportId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("📅 Historial del reporte {} entre {} y {} (página {}, tamaño {})", reportId, startDate, endDate, page, size);
        return ResponseEntity.ok(historyService.getHistoryByDateRange(reportId, startDate, endDate, page, size));
    }


    /**
     * Retorna el historial de un reporte filtrado por su estado anterior, con soporte de paginación.
     *
     * @param reportId       ID del reporte.
     * @param previousStatus Estado anterior que se desea filtrar.
     * @param page           Número de página (comienza en 1).
     * @param size           Tamaño de la página de resultados.
     * @return Lista paginada de {@link ReportStatusHistoryResponse} filtrada por estado anterior.
     */
    @GetMapping("/by-report/previous-status")
    public ResponseEntity<PaginatedHistoryResponse> getByPreviousStatus(
            @RequestParam String reportId,
            @RequestParam ReportStatus previousStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("⏪ Historial del reporte {} filtrado por estado anterior {} (página {}, tamaño {})",
                reportId, previousStatus, page, size);
        return ResponseEntity.ok(historyService.getHistoryByPreviousStatus(reportId, previousStatus, page, size));
    }


    /**
     * Retorna el historial de cambios de un reporte, filtrado por un estado nuevo específico
     * y un rango de fechas, con paginación.
     *
     * @param reportId  ID del reporte.
     * @param newStatus Estado nuevo que se desea filtrar.
     * @param startDate Fecha de inicio del rango.
     * @param endDate   Fecha de fin del rango.
     * @param page      Número de página (comienza en 1).
     * @param size      Tamaño de la página de resultados.
     * @return Lista de {@link ReportStatusHistoryResponse} con los registros que cumplen los filtros.
     */
    @GetMapping("/by-report/new-status-and-dates")
    public ResponseEntity<PaginatedHistoryResponse> getByNewStatusAndDateRange(
            @RequestParam String reportId,
            @RequestParam ReportStatus newStatus,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("✅ Historial del reporte {} con estado nuevo {} entre {} y {} (página {}, tamaño {})",
                reportId, newStatus, startDate, endDate, page, size);
        return ResponseEntity.ok(
                historyService.getHistoryByNewStatusAndDateRange(reportId, newStatus, startDate, endDate, page, size));
    }


    /**
     * Recupera el historial de todos los cambios de estado realizados por un usuario específico,
     * con soporte de paginación.
     *
     * @param userId ID del usuario.
     * @param page   Número de página (comienza en 1).
     * @param size   Tamaño de la página de resultados.
     * @return Lista de {@link ReportStatusHistoryResponse} relacionados al usuario.
     */
    @GetMapping("/by-user")
    public ResponseEntity<PaginatedHistoryResponse> getByUserId(
            @RequestParam String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("👤 Historial realizado por el usuario {} (página {}, tamaño {})", userId, page, size);
        return ResponseEntity.ok(historyService.getHistoryByUserId(userId, page, size));
    }


    /**
     * Cuenta la cantidad total de cambios de estado que ha tenido un reporte específico.
     *
     * @param reportId ID del reporte.
     * @return Número total de cambios de estado registrados para ese reporte.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countByReportId(@RequestParam String reportId) {
        log.info("🔢 Contando cambios de estado para el reporte {}", reportId);
        long count = historyService.countHistoryByReportId(reportId);
        return ResponseEntity.ok(count);
    }


}
