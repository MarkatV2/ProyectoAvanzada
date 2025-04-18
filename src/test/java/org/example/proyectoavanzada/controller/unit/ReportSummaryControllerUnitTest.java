package org.example.proyectoavanzada.controller.unit;


import co.edu.uniquindio.proyecto.controller.ReportController;
import co.edu.uniquindio.proyecto.controller.ReportSummaryController;
import co.edu.uniquindio.proyecto.dto.report.PaginatedReportSummaryResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportFilterDTO;
import co.edu.uniquindio.proyecto.dto.report.ReportSummaryDTO;
import co.edu.uniquindio.proyecto.exception.report.CreatingReportSummaryPdfException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import co.edu.uniquindio.proyecto.exceptionhandler.global.GlobalExceptionHandler;
import co.edu.uniquindio.proyecto.exceptionhandler.report.ReportExceptionHandler;
import co.edu.uniquindio.proyecto.service.interfaces.ReportSummaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.proyectoavanzada.configuration.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReportSummaryController.class)
@Import({ReportExceptionHandler.class, GlobalExceptionHandler.class, ErrorResponseBuilder.class})
@ContextConfiguration(classes = {ReportSummaryController.class, TestSecurityConfig.class})
class ReportSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportSummaryService reportSummaryService;

    @Autowired
    private ObjectMapper objectMapper;

    private ReportFilterDTO validFilter;
    private List<ReportSummaryDTO> fiveSummaries;

    @BeforeEach
    void setUp() {
        // Filtro válido
        validFilter = new ReportFilterDTO(
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now(),
                List.of("cat1","cat2"),
                new GeoJsonPoint(-75.0,4.0),
                10.0
        );
        // Cinco reportes de ejemplo
        fiveSummaries = IntStream.rangeClosed(1, 5).mapToObj(i ->
                new ReportSummaryDTO(
                        "id" + i,
                        "Title" + i,
                        "Desc" + i,
                        List.of("cat" + i),
                        "VERIFIED",
                        LocalDateTime.now().minusDays(i),
                        4.0 + i * 0.01,
                        -75.0 - i * 0.01
                )
        ).toList();
    }

    @Test
    @DisplayName("POST /api/v1/admin/reportSummaries/pdf retorna 200 y PDF válido")
    void generatePdf_Success() throws Exception {
        int page = 2, size = 3;
        long total = fiveSummaries.size();
        int totalPages = (int)Math.ceil((double)total/size);
        PaginatedReportSummaryResponse paginated =
                new PaginatedReportSummaryResponse(fiveSummaries, page, size, total, totalPages);
        byte[] pdfBytes = new byte[]{1,2,3,4};

        when(reportSummaryService.getFilteredReports(validFilter, page, size)).thenReturn(paginated);
        when(reportSummaryService.generatePdf(paginated)).thenReturn(pdfBytes);

        mockMvc.perform(post("/api/v1/admin/reportSummaries/pdf")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilter)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("reporte_" + LocalDateTime.now().toLocalDate() + ".pdf")
                                .build().toString()))
                .andExpect(content().bytes(pdfBytes));

        verify(reportSummaryService).getFilteredReports(validFilter, page, size);
        verify(reportSummaryService).generatePdf(paginated);
    }

    @Test
    @DisplayName("POST /api/v1/admin/reportSummaries/pdf retorna 400 cuando filtro inválido")
    void generatePdf_InvalidFilter() throws Exception {
        // Simula error de validación de fechas
        when(reportSummaryService.getFilteredReports(any(), anyInt(), anyInt()))
                .thenThrow(new IllegalArgumentException("Fechas inválidas"));

        mockMvc.perform(post("/api/v1/admin/reportSummaries/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilter)))
                .andExpect(status().isBadRequest());

        verify(reportSummaryService).getFilteredReports(validFilter, 1, 20);
        verify(reportSummaryService, never()).generatePdf(any());
    }

    @Test
    @DisplayName("POST /api/v1/admin/reportSummaries/pdf retorna 500 cuando falla la generación de PDF")
    void generatePdf_PdfGenerationError() throws Exception {
        // Simula éxito del filtrado, pero fallo al generar PDF
        PaginatedReportSummaryResponse paginated =
                new PaginatedReportSummaryResponse(fiveSummaries, 1, 20, fiveSummaries.size(), 1);
        when(reportSummaryService.getFilteredReports(validFilter, 1, 20)).thenReturn(paginated);
        when(reportSummaryService.generatePdf(paginated))
                .thenThrow(new CreatingReportSummaryPdfException("Error interno"));

        mockMvc.perform(post("/api/v1/admin/reportSummaries/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFilter)))
                .andExpect(status().isInternalServerError());

        verify(reportSummaryService).getFilteredReports(validFilter, 1, 20);
        verify(reportSummaryService).generatePdf(paginated);
    }

    @Test
    @DisplayName("POST /api/v1/admin/reportSummaries/pdf retorna 400 cuando falta cuerpo de la petición")
    void generatePdf_MissingBody() throws Exception {
        mockMvc.perform(post("/api/v1/admin/reportSummaries/pdf"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reportSummaryService);
    }
}

