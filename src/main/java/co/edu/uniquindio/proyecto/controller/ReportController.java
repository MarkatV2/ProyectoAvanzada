package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.comment.CommentPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.report.PaginatedReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportRequest;
import co.edu.uniquindio.proyecto.dto.report.ReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusUpdate;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.service.implementations.ReportServiceImplements;
import co.edu.uniquindio.proyecto.annotation.CheckOwnerOrAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// Controlador actualizado
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportServiceImplements reportService;//FALTA LOS METODOS PATCH Y PUT

    @GetMapping
    public ResponseEntity<PaginatedReportResponse> filtrarReportes(@RequestParam double latitud, @RequestParam double longitud,
            @RequestParam(required = false) Double radio, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        log.info("Solicitud de obtener reportes - Lat: {}, Lon: {}", latitud, longitud);
        PaginatedReportResponse response = reportService.getReportsNearLocation(latitud, longitud, radio, page, size);
        return ResponseEntity.ok(response);
    }


    //METODO para obtener todas las imagenes de un reporte
    @GetMapping("/{reportId}/images")
    public ResponseEntity<List<ImageResponse>> getAllImagesByReport(@PathVariable String reportId) {
        log.info("Solicitando todas las imagenes del reporte con id: {}", reportId);
        return ResponseEntity.ok(reportService.getAllImagesByReport(reportId));
    }


    @GetMapping("/{reportId}")
    public ResponseEntity<ReportResponse> getReport(
            @PathVariable String reportId) {
        log.info("Solicitud para obtener reporte con ID: {}", reportId);
        ReportResponse response = reportService.getReportById(reportId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ReportResponse> createReport( //Manejar el Id del usuario
            @Valid @RequestBody ReportRequest request) {

        log.info("Creando un nuevo reporte {}", request.title());

        ReportResponse response = reportService.createReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    /**
     * Cambia el estado de un reporte, validando si el usuario tiene permisos.
     *
     * @param reportId ID del reporte a actualizar.
     * @param dto Datos con el nuevo estado y mensaje (si aplica).
     */
    @PatchMapping("/{reportId}/status")
    @CheckOwnerOrAdmin(entityClass = Report.class)
    public ResponseEntity<Void> updateReportStatus(
            @PathVariable ObjectId reportId,
            @RequestBody ReportStatusUpdate dto) {
        reportService.updateReportStatus(reportId, dto);
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/{reportId}")
    @CheckOwnerOrAdmin(entityClass = Report.class)
    public ResponseEntity<Void> deleteReport(
            @PathVariable String reportId) {

        log.info("Solicitud de eliminación de reporte ID: {}", reportId);

        reportService.softDeleteReport(reportId);
        log.debug("Reporte {} eliminado exitosamente", reportId);

        return ResponseEntity.noContent().build();
    }


    @PutMapping("/{reportId}")
    @CheckOwnerOrAdmin(entityClass = Report.class) //devolver la ubicion
    public ResponseEntity<ReportResponse> updateReport(
            @PathVariable String reportId,
            @RequestBody @Valid ReportRequest request) {

        ReportResponse response = reportService.updateReport(new ObjectId(reportId), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para alternar (toggle) el voto (like) de un reporte.
     * Si el usuario no ha votado, se suma un voto; si ya votó, se remueve.
     *
     * @param reportId Identificador del reporte.
     * @return Respuesta sin contenido.
     */
    @PatchMapping("/{reportId}/votes")
    public ResponseEntity<Void> toggleVote(@PathVariable String reportId) {
        log.debug("intentando alternar el voto para el reporte {}", reportId);

        reportService.toggleReportVote(reportId);
        return ResponseEntity.noContent().build();
    }



    /**
     * Endpoint para obtener todos los comentarios asociados a un reporte de forma paginada.
     *
     * @param reportId Identificador del reporte.
     * @param page     Número de página (opcional, por defecto 0).
     * @param size     Tamaño de página (opcional, por defecto 10).
     * @return ResponseEntity con el CommentPaginatedResponse.
     */
    @GetMapping("/{reportId}/comments")
    public ResponseEntity<CommentPaginatedResponse> getCommentsByReport(
            @PathVariable String reportId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Recibida petición para obtener comentarios del reporte con ID: {} (página: {}, tamaño: {})", reportId, page, size);
        CommentPaginatedResponse response = reportService.getCommentsByReportId(reportId, page, size);
        return ResponseEntity.ok(response);
    }


}

