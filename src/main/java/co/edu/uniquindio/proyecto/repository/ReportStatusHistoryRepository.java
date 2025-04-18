package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.entity.report.ReportStatusHistory;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;

/**
 * Repositorio para la gestión del historial de cambios de estado de reportes.
 * <p>
 * Esta interfaz proporciona operaciones CRUD y consultas personalizadas para acceder a los registros
 * de cambios de estado de reportes almacenados en MongoDB. Permite la trazabilidad y auditoría
 * de los reportes, así como una visualización ordenada de su evolución.
 *
 * <p><strong>Responsabilidad única:</strong> esta interfaz se encarga exclusivamente del acceso
 * a datos de {@link ReportStatusHistory}.</p>
 */
public interface ReportStatusHistoryRepository extends MongoRepository<ReportStatusHistory, ObjectId> {

    /**
     * Obtiene el historial de cambios de estado de un reporte específico.
     *
     * @param reportId El ID del reporte cuyo historial de cambios de estado se desea obtener.
     * @param pageable Información de paginación.
     * @return Una página de registros de historial de cambios de estado.
     */
    @Query("{ 'reportId': ?0 }")
    Page<ReportStatusHistory> findByReportId(ObjectId reportId, Pageable pageable);


    /**
     * Obtiene el historial de cambios de estado de un reporte específico dentro de un rango de fechas.
     *
     * @param reportId El ID del reporte cuyo historial de cambios de estado se desea obtener.
     * @param startDate La fecha de inicio del rango.
     * @param endDate La fecha de fin del rango.
     * @param pageable Información de paginación.
     * @return Una página de registros de historial de cambios de estado en el rango de fechas especificado.
     */
    @Query("{ 'reportId': ?0, 'changedAt': { $gte: ?1, $lte: ?2 } }")
    Page<ReportStatusHistory> findByReportIdAndDateRange(ObjectId reportId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);


    /**
     * Obtiene el historial de cambios de estado de un reporte filtrado por el estado anterior.
     *
     * @param reportId El ID del reporte cuyo historial de cambios de estado se desea obtener.
     * @param previousStatus El estado anterior del reporte.
     * @param pageable Información de paginación.
     * @return Una página de registros de historial de cambios de estado filtrados por el estado anterior.
     */
    @Query("{ 'reportId': ?0, 'previousStatus': ?1 }")
    Page<ReportStatusHistory> findByReportIdAndPreviousStatus(ObjectId reportId, ReportStatus previousStatus, Pageable pageable);


    /**
     * Obtiene el historial de cambios de estado de un reporte específico filtrado por el usuario que realizó el cambio.
     *
     * @param userId El ID del usuario que realizó el cambio de estado.
     * @param pageable Información de paginación.
     * @return Una página de registros de historial de cambios de estado filtrados por usuario.
     */
    @Query("{ 'userId': ?0 }")
    Page<ReportStatusHistory> findByUserId(ObjectId userId, Pageable pageable);


    /**
     * Cuenta cuántos cambios de estado ha tenido un reporte.
     *
     * @param reportId El ID del reporte cuyo número de cambios de estado se desea contar.
     * @return El número total de cambios de estado que ha tenido el reporte.
     */
    long countByReportId(ObjectId reportId);


    /**
     * Obtiene el historial de cambios de estado de un reporte filtrado por el nuevo estado y un rango de fechas.
     *
     * @param reportId El ID del reporte cuyo historial de cambios de estado se desea obtener.
     * @param newStatus El nuevo estado del reporte.
     * @param startDate La fecha de inicio del rango.
     * @param endDate La fecha de fin del rango.
     * @param pageable Información de paginación.
     * @return Una página de registros de historial de cambios de estado filtrados por el nuevo estado y el rango de fechas.
     */
    @Query("{ 'reportId': ?0, 'newStatus': ?1, 'changedAt': { $gte: ?2, $lte: ?3 } }")
    Page<ReportStatusHistory> findByReportIdAndNewStatusAndDateRange(
            ObjectId reportId,
            ReportStatus newStatus,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

}
