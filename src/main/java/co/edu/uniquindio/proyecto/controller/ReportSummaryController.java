package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.report.ReportFilterDTO;
import co.edu.uniquindio.proyecto.dto.report.ReportSummaryDTO;
import co.edu.uniquindio.proyecto.service.interfaces.ReportSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST para generar informes de reportes en formato PDF.
 * <p>
 * Esta funcionalidad está restringida a usuarios con rol de administrador.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/admin/reportSummaries")
@RequiredArgsConstructor
@Slf4j
public class ReportSummaryController {

    private final ReportSummaryService reportSummaryService;

    /**
     * Genera un archivo PDF con un resumen de reportes, filtrado por fecha, categoría o ubicación.
     *
     * @param filter Criterios de filtrado para el informe.
     * @return Archivo PDF generado, adjunto en la respuesta.
     */
    @PostMapping("/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> generatePdf(@RequestBody ReportFilterDTO filter) {
        log.info("📄 Generando resumen PDF con filtros: {}", filter);

        List<ReportSummaryDTO> reports = reportSummaryService.getFilteredReports(filter);
        byte[] pdf = reportSummaryService.generatePdf(reports);

        String filename = String.format("reporte_%s.pdf", LocalDate.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build()
        );

        log.info("✅ Informe PDF generado correctamente: {}", filename);
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}