package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.comment.CommentPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.report.*;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.annotation.CheckOwnerOrAdmin;
import co.edu.uniquindio.proyecto.service.interfaces.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.access.prepost.PreAuthorize;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST para la gestión de reportes de usuarios.
 * <p>
 * Permite crear, actualizar, consultar, eliminar y votar reportes.
 * También expone endpoints para obtener comentarios e imágenes asociadas.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * Filtra y retorna reportes cercanos a una ubicación geográfica dada.
     *
     * @param latitud  Latitud del punto de referencia.
     * @param longitud Longitud del punto de referencia.
     * @param radio    Radio de búsqueda en kilómetros (opcional).
     * @param page     Página de resultados (opcional).
     * @param size     Tamaño de página (opcional).
     * @return Lista paginada de reportes cercanos.
     */
    @GetMapping
    public ResponseEntity<PaginatedReportResponse> filtrarReportes(
            @RequestParam double latitud,
            @RequestParam double longitud,
            @RequestParam(required = false) Double radio,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> categories ) {

        log.info("📍 Obteniendo reportes cerca de: ({}, {}), radio: {}km", latitud, longitud, radio);
        PaginatedReportResponse response = reportService.getReportsNearLocation(latitud, longitud, radio, page, size, categories);
        return ResponseEntity.ok(response);
    }

        /**
     * Retorna todos los reportes activos (excluyendo los eliminados), con paginación.
     * Solo accesible por usuarios con rol ADMIN.
     *
     * @param page Número de página (opcional, por defecto 1).
     * @param size Tamaño de página (opcional, por defecto 30, máximo 100).
     * @return Lista paginada de reportes activos.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaginatedReportResponse> getAllReportsAdmin(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        log.info("📋 Listando todos los reportes (page={}, size={})", page, size);
        PaginatedReportResponse response = reportService.getAllReports(page, size);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/my")
    public ResponseEntity<PaginatedReportResponse> getAllReportsAdminByUser(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        log.info("📋 Listando todos los reportes (page={}, size={})", page, size);
        PaginatedReportResponse response = reportService.getAllReportsByUserId(page, size);
        return ResponseEntity.ok(response);
    }


    /**
     * Obtiene un reporte específico por su ID.
     *
     * @param reportId ID del reporte.
     * @return Reporte encontrado.
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportResponse> getReport(@PathVariable String reportId) {
        log.info("🔍 Buscando reporte con ID: {}", reportId);
        ReportResponse response = reportService.getReportById(reportId);
        return ResponseEntity.ok(response);
    }

    /**
     * Crea un nuevo reporte.
     *
     * @param request Datos del nuevo reporte.
     * @return Reporte creado con HTTP 201.
     */
    @PostMapping
    public ResponseEntity<ReportResponse> createReport(@Valid @RequestBody ReportRequest request) {
        log.info("🆕 Creando reporte: {}", request.title());
        ReportResponse response = reportService.createReport(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Actualiza un reporte existente.
     *
     * @param reportId ID del reporte.
     * @param request  Datos nuevos.
     * @return Reporte actualizado.
     */
    @PutMapping("/{reportId}")
    @CheckOwnerOrAdmin(entityClass = Report.class)
    public ResponseEntity<ReportResponse> updateReport(
            @PathVariable String reportId,
            @Valid @RequestBody ReportUpdateDto request) {

        log.info("✏️ Actualizando reporte con ID: {}", reportId);
        ReportResponse response = reportService.updateReport(reportId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .build()
                .toUri();

        return ResponseEntity.ok()
                .header(HttpHeaders.LOCATION, location.toString())
                .body(response);
    }


    /**
     * Elimina lógicamente (soft delete) un reporte.
     *
     * @param reportId ID del reporte.
     * @return HTTP 204 si fue eliminado correctamente.
     */
    @DeleteMapping("/{reportId}")
    @CheckOwnerOrAdmin(entityClass = Report.class)
    public ResponseEntity<Void> deleteReport(@PathVariable String reportId) {
        log.info("🗑️ Eliminando reporte con ID: {}", reportId);
        reportService.softDeleteReport(reportId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cambia el estado de un reporte (Ej. a Resuelto, Verificado, etc.).
     *
     * @param reportId ID del reporte.
     * @param dto      Estado y mensaje adicional.
     */
    @PatchMapping("/{reportId}/status")
    @CheckOwnerOrAdmin(entityClass = Report.class)
    public ResponseEntity<Void> updateReportStatus(
            @PathVariable String reportId,
            @RequestBody ReportStatusUpdate dto) {
        log.info("🔁 Cambiando estado del reporte {} a {}", reportId, dto.status());
        reportService.updateReportStatus(reportId, dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Alterna el voto "importante" de un usuario sobre un reporte.
     *
     * @param reportId ID del reporte.
     * @return HTTP 204 si se alternó correctamente.
     */
    @PatchMapping("/{reportId}/votes")
    public ResponseEntity<Void> toggleVote(@PathVariable String reportId) {
        log.debug("👍 Alternando voto para el reporte {}", reportId);
        reportService.toggleReportVote(reportId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene todas las imágenes asociadas a un reporte.
     *
     * @param reportId ID del reporte.
     * @return Lista de imágenes.
     */
    @GetMapping("/{reportId}/images")
    public ResponseEntity<List<ImageResponse>> getAllImagesByReport(@PathVariable String reportId) {
        log.info("🖼️ Consultando imágenes del reporte ID: {}", reportId);
        return ResponseEntity.ok(reportService.getAllImagesByReport(reportId));
    }

    /**
     * Obtiene los comentarios de un reporte de forma paginada.
     *
     * @param reportId ID del reporte.
     * @param page     Página (por defecto 0).
     * @param size     Tamaño de página (por defecto 10).
     * @return Comentarios paginados.
     */
    @GetMapping("/{reportId}/comments")
    public ResponseEntity<CommentPaginatedResponse> getCommentsByReport(
            @PathVariable String reportId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("💬 Comentarios del reporte {} (página {}, tamaño {})", reportId, page, size);
        CommentPaginatedResponse response = reportService.getCommentsByReportId(reportId, page, size);
        return ResponseEntity.ok(response);
    }

}