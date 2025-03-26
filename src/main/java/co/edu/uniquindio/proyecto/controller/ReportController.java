package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.report.PaginatedReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportRequest;
import co.edu.uniquindio.proyecto.dto.report.ReportResponse;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.service.implementations.ReportServiceImplements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.print.Pageable;
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


    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> deleteReport(
            @PathVariable String reportId) {

        log.info("Solicitud de eliminación de reporte ID: {}", reportId);

        reportService.softDeleteReport(reportId);
        log.debug("Reporte {} eliminado exitosamente", reportId);

        return ResponseEntity.noContent().build();
    }

}

