package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.report.PaginatedHistoryResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusHistoryResponse;
import co.edu.uniquindio.proyecto.entity.report.ReportStatusHistory;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.exception.report.HistoryNotFoundException;
import co.edu.uniquindio.proyecto.exception.report.ReportNotFoundException;
import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.repository.ReportRepository;
import co.edu.uniquindio.proyecto.repository.ReportStatusHistoryRepository;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.service.interfaces.ReportStatusHistoryService;
import co.edu.uniquindio.proyecto.service.mapper.ReportStatusHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Servicio para la gestión del historial de estados de reportes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportStatusHistoryServiceImpl implements ReportStatusHistoryService {

    private final ReportStatusHistoryRepository historyRepository;
    private final ReportStatusHistoryMapper historyMapper;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    /**
     * Crea internamente una entrada de historial.
     *
     * @param reportId       ID del reporte.
     * @param userId         ID del usuario que realiza el cambio.
     * @param previousStatus Estado anterior.
     * @param newStatus      Nuevo estado.
     */
    @Override
    public void createHistory(ObjectId reportId, ObjectId userId, ReportStatus previousStatus, ReportStatus newStatus) {
        ReportStatusHistory reportStatusHistory = historyMapper.toEntity(reportId, userId, previousStatus, newStatus);
        log.info("Se creó historial para reporte {}: {} -> {} por el usuario {}",
                reportId, previousStatus, newStatus, userId);
        historyRepository.save(reportStatusHistory);
    }


    @Override
    public PaginatedHistoryResponse getAllHistories(int page, int size) {
        log.info("Solicitando todos los historiales completo. Página: {}, Tamaño: {}", page, size);
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<ReportStatusHistory> result =
                historyRepository.findAll(pageable);
        return toPaginatedHistoryResponse(result, page, size);
    }


    /**
     * Obtiene un historial específico por su ID.
     *
     * @param historyId ID del historial.
     * @return ReportStatusHistoryResponse del historial encontrado.
     * @throws HistoryNotFoundException Si no se encuentra el historial.
     */
    @Override
    public ReportStatusHistoryResponse getHistoryById(String historyId) {
        log.info("Obteniendo historial con ID: {}", historyId);
        ReportStatusHistory history = historyRepository.findById(new ObjectId(historyId))
                .orElseThrow(() -> {
                    log.error("Historial no encontrado con ID: {}", historyId);
                    return new HistoryNotFoundException("Historial no encontrado con ID: " + historyId);
                });
        return historyMapper.toResponse(history);
    }


    /**
     * Obtiene el historial completo de cambios de estado de un reporte.
     *
     * @param reportId ID del reporte.
     * @param page     Página de resultados (0-based).
     * @param size     Tamaño de página.
     * @return Lista paginada de historial de cambios de estado.
     */
    @Override
    public PaginatedHistoryResponse getHistoryByReportId(String reportId, int page, int size) {
        log.info("Consultando si existe el reporte {} ....", reportId);
        reportRepository.findById(new ObjectId(reportId)).orElseThrow(() -> new ReportNotFoundException(reportId));
        log.info("Solicitando historial completo para el reporte {}. Página: {}, Tamaño: {}", reportId, page, size);
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<ReportStatusHistory> result =
                historyRepository.findByReportId(new ObjectId(reportId), pageable);
        return toPaginatedHistoryResponse(result, page, size);
    }

    /**
     * Obtiene el historial de cambios de estado realizados por un usuario específico.
     *
     * @param userId ID del usuario.
     * @param page   Página de resultados.
     * @param size   Tamaño de página.
     * @return Lista de historial filtrado por usuario.
     */
    @Override
    public PaginatedHistoryResponse getHistoryByUserId(String userId, int page, int size) {
        log.info("Consultando si existe el usuario con id: {} ...", userId);
        userRepository.findById(new ObjectId(userId)).orElseThrow(() -> new UserNotFoundException(userId));
        log.info("Obteniendo historial de cambios realizados por el usuario {}. Página: {}, Tamaño: {}", userId, page, size);
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<ReportStatusHistory> result =
                historyRepository.findByUserId(new ObjectId(userId), pageable);
        return toPaginatedHistoryResponse(result, page, size);
    }

    /**
     * Obtiene el historial de un reporte filtrado por su estado anterior.
     *
     * @param reportId        ID del reporte.
     * @param previousStatus  Estado anterior.
     * @param page            Página de resultados.
     * @param size            Tamaño de página.
     * @return Lista paginada filtrada por estado anterior.
     */
    @Override
    public PaginatedHistoryResponse getHistoryByPreviousStatusAndReportId(String reportId, ReportStatus previousStatus, int page, int size) {
        log.info("Historial filtrado por estado anterior '{}' para reporte {}. Página: {}, Tamaño: {}", previousStatus, reportId, page, size);
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<ReportStatusHistory> result =
                historyRepository.findByReportIdAndPreviousStatus(new ObjectId(reportId), previousStatus, pageable);
        return toPaginatedHistoryResponse(result, page, size);
    }

    @Override
    public PaginatedHistoryResponse getHistoryByPreviousStatus(ReportStatus previousStatus, int page, int size) {
        log.info("Historial filtrado por estado anterior '{}' Página: {}, Tamaño: {}", previousStatus, page, size);
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<ReportStatusHistory> result =
                historyRepository.findByPreviousStatus(previousStatus, pageable);
        return toPaginatedHistoryResponse(result, page, size);
    }

    @Override
    public PaginatedHistoryResponse getHistoryByNewStatus(ReportStatus newStatus, int page, int size) {
        log.info("Historial filtrado por estado anterior '{}' Página: {}, Tamaño: {}", newStatus, page, size);
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<ReportStatusHistory> result =
                historyRepository.findByNewStatus(newStatus, pageable);
        return toPaginatedHistoryResponse(result, page, size);
    }

    /**
     * Obtiene el historial de cambios de estado de un reporte filtrado por nuevo estado y rango de fechas.
     *
     * @param reportId  ID del reporte.
     * @param newStatus Nuevo estado del reporte.
     * @param startDate Fecha de inicio.
     * @param endDate   Fecha de fin.
     * @param page      Página.
     * @param size      Tamaño.
     * @return Lista paginada de historial.
     */
    @Override
    public PaginatedHistoryResponse getHistoryByNewStatusAndDateRange(
            String reportId, ReportStatus newStatus, LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        log.info("Historial filtrado por estado '{}' y fechas [{} - {}] para reporte {}",
                newStatus, startDate, endDate, reportId);
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<ReportStatusHistory> result =
                historyRepository.findByReportIdAndNewStatusAndDateRange(
                        new ObjectId(reportId), newStatus, startDate, endDate, pageable);
        return toPaginatedHistoryResponse(result, page, size);
    }

    /**
     * Cuenta cuántos cambios de estado ha tenido un reporte.
     *
     * @param reportId ID del reporte.
     * @return Número total de cambios.
     */
    @Override
    public long countHistoryByReportId(String reportId) {
        long count = historyRepository.countByReportId(new ObjectId(reportId));
        log.info("El reporte {} ha tenido {} cambios de estado", reportId, count);
        return count;
    }


    /**
     * Obtiene el historial de cambios de estado de un reporte dentro de un rango de fechas,
     * con paginación.
     *
     * @param reportId  El ObjectId del reporte.
     * @param startDate La fecha de inicio.
     * @param endDate   La fecha de fin.
     * @param page      Número de página (0-based).
     * @param size      Tamaño de página.
     * @return Una página de registros filtrados por el rango de fechas.
     */
    @Override
    public PaginatedHistoryResponse getHistoryByDateRange(String reportId, LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        log.info("Solicitando historial para el reporte {} desde {} hasta {}. Página: {}, Tamaño: {}",
                reportId, startDate, endDate, page, size);
        Pageable pageable = PageRequest.of(page-1, size);
        Page<ReportStatusHistory> result =
                historyRepository.findByReportIdAndDateRange(new ObjectId(reportId), startDate, endDate, pageable);
        log.info("Historial filtrado por fecha: {} registros totales", result.getTotalElements());
        return toPaginatedHistoryResponse(result, page, size);
    }


    private PaginatedHistoryResponse toPaginatedHistoryResponse(Page<ReportStatusHistory> result, int page, int size){
        return new PaginatedHistoryResponse(
                historyMapper.toListResponse(result.getContent()),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

}
