package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.report.ReportStatusHistoryResponse;
import co.edu.uniquindio.proyecto.entity.report.ReportStatusHistory;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.exception.report.HistoryNotFoundException;
import co.edu.uniquindio.proyecto.repository.ReportStatusHistoryRepository;
import co.edu.uniquindio.proyecto.service.mapper.ReportStatusHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

/**
 * Servicio para la gestión del historial de estados de reportes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportStatusHistoryService {

    private final ReportStatusHistoryRepository historyRepository;
    private final ReportStatusHistoryMapper historyMapper;

    /**
     * Crea internamente una entrada de historial.
     *
     * @param reportId       ID del reporte.
     * @param userId         ID del usuario que realiza el cambio.
     * @param previousStatus Estado anterior.
     * @param newStatus      Nuevo estado.
     */
    public void createHistory(ObjectId reportId, ObjectId userId, ReportStatus previousStatus, ReportStatus newStatus) {
        ReportStatusHistory history = historyMapper.toEntity(reportId, userId, previousStatus, newStatus);
        log.info("Se creó historial para reporte {}: {} -> {} por el usuario {}",
                reportId, previousStatus, newStatus, userId);
    }

    /**
     * Obtiene un historial específico por su ID.
     *
     * @param historyId ID del historial.
     * @return ReportStatusHistoryResponse del historial encontrado.
     * @throws HistoryNotFoundException Si no se encuentra el historial.
     */
    public ReportStatusHistoryResponse getHistoryById(String historyId) {
        log.info("Obteniendo historial con ID: {}", historyId);
        ReportStatusHistory history = historyRepository.findById(new ObjectId(historyId))
                .orElseThrow(() -> {
                    log.error("Historial no encontrado con ID: {}", historyId);
                    return new HistoryNotFoundException("Historial no encontrado con ID: " + historyId);
                });
        return historyMapper.toResponse(history);
    }

    /*
     * Obtiene de forma paginada el historial de estados para un reporte.
     *
     * @param reportId ID del reporte.
     * @param pageable Parámetros de paginación.
     * @return ReportStatusHistoryPaginatedResponse con el historial.
     * @throws ResourceNotFoundException Si no se encuentra ningún historial para el reporte.

    public ReportStatusHistoryPaginatedResponse getHistoriesByReportId(String reportId, Pageable pageable) {
        log.info("Obteniendo historial para el reporte con ID: {}", reportId);
        Page<ReportStatusHistory> page = historyRepository.findByReportId(new ObjectId(reportId), pageable);
        if (page.isEmpty()) {
            log.warn("No se encontraron historiales para el reporte con ID: {}", reportId);
            throw new HystoryNotFoundException("No se encontraron historiales para el reporte con ID: " + reportId);
        }
        List<ReportStatusHistoryResponse> content = page.getContent()
                .stream()
                .map(historyMapper::toResponse)
                .collect(Collectors.toList());
        return new ReportStatusHistoryPaginatedResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }*/
}
