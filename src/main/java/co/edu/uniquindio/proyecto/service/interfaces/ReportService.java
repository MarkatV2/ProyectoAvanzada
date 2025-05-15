package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.annotation.CheckOwnerOrAdmin;
import co.edu.uniquindio.proyecto.dto.comment.CommentPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.report.*;
import co.edu.uniquindio.proyecto.entity.report.Report;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
/**
 * Servicio para gestionar reportes ciudadanos.
 */
public interface ReportService {

    /**
     * Crea un nuevo reporte.
     *
     * @param request datos del reporte.
     * @return reporte creado.
     */
    ReportResponse createReport(ReportRequest request);


    @PreAuthorize("hasRole('ADMIN')")
    PaginatedReportResponse getAllReports(Integer page, Integer size);

    /**
     * Obtiene un reporte por su ID.
     *
     * @param id ID del reporte.
     * @return reporte encontrado.
     */
    ReportResponse getReportById(String id);

    /**
     * Obtiene reportes cercanos a una ubicación geográfica.
     *
     * @param latitude   latitud del punto de búsqueda.
     * @param longitude  longitud del punto de búsqueda.
     * @param radiusKm   radio en kilómetros (puede ser nulo).
     * @param page       número de página.
     * @param size       tamaño de página.
     * @return lista paginada de reportes cercanos.
     */
    PaginatedReportResponse getReportsNearLocation(double latitude, double longitude, Double radiusKm,
                                                   Integer page, Integer size, List<String> categories);

    PaginatedReportResponse getAllReportsByUserId(Integer page, Integer size);

    /**
     * Elimina lógicamente un reporte.
     *
     * @param reportId ID del reporte.
     */

    @CheckOwnerOrAdmin(entityClass = Report.class)
    void softDeleteReport(String reportId);

    /**
     * Actualiza un reporte existente.
     *
     * @param reportId ID del reporte.
     * @param request  nuevos datos del reporte.
     * @return reporte actualizado.
     */

    @CheckOwnerOrAdmin(entityClass = Report.class)
    ReportResponse updateReport(String reportId, ReportUpdateDto request);

    /**
     * Actualiza el estado de un reporte.
     *
     * @param reportId ID del reporte.
     * @param dto      nuevo estado.
     */

    @CheckOwnerOrAdmin(entityClass = Report.class)
    void updateReportStatus(String reportId, ReportStatusUpdate dto);

    /**
     * Cambia el voto del usuario sobre un reporte (agrega o quita voto).
     *
     * @param reportId ID del reporte.
     */
    boolean toggleReportVote(String reportId);

    /**
     * Obtiene los comentarios asociados a un reporte de forma paginada.
     *
     * @param reportId ID del reporte.
     * @param page     número de página.
     * @param size     tamaño de página.
     * @return comentarios paginados.
     */
    CommentPaginatedResponse getCommentsByReportId(String reportId, int page, int size);

    /**
     * Obtiene las imágenes asociadas a un reporte.
     *
     * @param reportId ID del reporte.
     * @return lista de imágenes.
     */
    List<ImageResponse> getAllImagesByReport(String reportId);
}
