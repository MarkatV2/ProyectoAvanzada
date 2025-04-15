package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.dto.comment.CommentPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.report.PaginatedReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportRequest;
import co.edu.uniquindio.proyecto.dto.report.ReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusUpdate;

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
     * @param categories
     * @return lista paginada de reportes cercanos.
     */
    PaginatedReportResponse getReportsNearLocation(double latitude, double longitude, Double radiusKm,
                                                   Integer page, Integer size, List<String> categories);

    /**
     * Elimina lógicamente un reporte.
     *
     * @param reportId ID del reporte.
     */
    void softDeleteReport(String reportId);

    /**
     * Actualiza un reporte existente.
     *
     * @param reportId ID del reporte.
     * @param request  nuevos datos del reporte.
     * @return reporte actualizado.
     */
    ReportResponse updateReport(String reportId, ReportRequest request);

    /**
     * Actualiza el estado de un reporte.
     *
     * @param reportId ID del reporte.
     * @param dto      nuevo estado.
     */
    void updateReportStatus(String reportId, ReportStatusUpdate dto);

    /**
     * Cambia el voto del usuario sobre un reporte (agrega o quita voto).
     *
     * @param reportId ID del reporte.
     */
    void toggleReportVote(String reportId);

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
