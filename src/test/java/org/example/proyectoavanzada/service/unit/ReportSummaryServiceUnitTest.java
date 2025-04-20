package org.example.proyectoavanzada.service.unit;

import co.edu.uniquindio.proyecto.dto.report.PaginatedReportSummaryResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportFilterDTO;
import co.edu.uniquindio.proyecto.dto.report.ReportSummaryDTO;
import co.edu.uniquindio.proyecto.entity.category.CategoryRef;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.service.implementations.ReportSummaryServiceImpl;
import co.edu.uniquindio.proyecto.service.mapper.ReportSummaryMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportSummaryServiceUnitTest {

    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private ReportSummaryMapper reportSummaryMapper;

    @InjectMocks private ReportSummaryServiceImpl service;

    private List<Report> testReports;
    private ReportFilterDTO validFilter;

    @BeforeEach
    void setUp() {

        // Crear 5 reportes de prueba con fechas válidas
        testReports = IntStream.range(1, 6).mapToObj(i -> {
            Report r = new Report();
            r.setId(new ObjectId());
            r.setCreatedAt(LocalDateTime.now().minusDays(6 - i));
            r.setCategoryList(List.of(new CategoryRef("cat" + i)));
            r.setLocation(new GeoJsonPoint(-75.0 + i * 0.01, 4.0 + i * 0.01));
            r.setTitle("Title" + i);
            r.setDescription("Desc" + i);
            r.setReportStatus(ReportStatus.VERIFIED);
            return r;
        }).toList();

        // Filtro válido: rango que incluye todos los reportes
        validFilter = new ReportFilterDTO(
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().plusDays(1),
                List.of(),                           // sin filtro de categoría
                new GeoJsonPoint(-75.0, 4.0),
                10.0                                 // radio amplio
        );
    }


    // ------------------------------------------- GET_FILTERED_REPORTS -------------------------------------------- //


    @Test
    @DisplayName("getFilteredReports - Debe retornar paginación correcta con resultados")
    void getFilteredReports_ShouldReturnPaginatedResult() {
        // Arrange
        int page = 1, size = 2;
        long total = testReports.size();
        // Count stub
        when(mongoTemplate.count(any(Query.class), eq(Report.class)))
                .thenReturn(total);
        // Find stub: devuelve primero 'size' elementos
        when(mongoTemplate.find(any(Query.class), eq(Report.class)))
                .thenReturn(testReports.subList(0, size));
        // Mapper stub: convierte domain -> DTO
        List<ReportSummaryDTO> dtos = List.of(
                new ReportSummaryDTO(testReports.get(0).getId().toHexString(), "Title1",  "Elalalala",
                        List.of("cat1"), "VERIFIED", testReports.get(0).getCreatedAt(), 4.01, -75.01),
                new ReportSummaryDTO(testReports.get(1).getId().toHexString(), "Title2",  "lalsfkdk",
                        List.of("cat2"), "VERIFIED", testReports.get(1).getCreatedAt(), 4.02, -75.02)
        );
        when(reportSummaryMapper.toReportSummaryDto(testReports.subList(0, size))).thenReturn(dtos);

        // Act
        PaginatedReportSummaryResponse resp = service.getFilteredReports(validFilter, page, size);

        // Assert
        assertNotNull(resp);
        assertEquals(page, resp.page());
        assertEquals(size, resp.size());
        assertEquals(total, resp.totalElements());
        assertEquals((int)Math.ceil((double)total/size), resp.totalPages());
        assertEquals(dtos, resp.content());

        verify(mongoTemplate).count(any(Query.class), eq(Report.class));
        verify(mongoTemplate).find(any(Query.class), eq(Report.class));
        verify(reportSummaryMapper).toReportSummaryDto(testReports.subList(0, size));
    }

    @Test
    @DisplayName("getFilteredReports - Debe retornar lista vacía cuando no hay coincidencias")
    void getFilteredReports_ShouldReturnEmpty_WhenNoMatches() {
        // Arrange
        int page = 1, size = 5;
        when(mongoTemplate.count(any(Query.class), eq(Report.class)))
                .thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Report.class)))
                .thenReturn(List.of());
        when(reportSummaryMapper.toReportSummaryDto(List.of())).thenReturn(List.of());

        // Act
        PaginatedReportSummaryResponse resp = service.getFilteredReports(validFilter, page, size);

        // Assert
        assertTrue(resp.content().isEmpty());
        assertEquals(0, resp.totalElements());
        verify(reportSummaryMapper).toReportSummaryDto(List.of());
    }


    // ------------------------------------------- GENERATED_PDF -------------------------------------------- //


    @Test
    @DisplayName("generatePdf - Debe generar un arreglo de bytes no vacío")
    void generatePdf_ShouldReturnNonEmptyPdf() {
        // Arrange
        List<ReportSummaryDTO> dummy = List.of(
                new ReportSummaryDTO("id1","T", "",List.of(),"VERIFIED",LocalDateTime.now(),4, -75),
                new ReportSummaryDTO("id2","T","", List.of(),"VERIFIED",LocalDateTime.now(),4, -75)
        );
        PaginatedReportSummaryResponse paginated =
                new PaginatedReportSummaryResponse(dummy, 1, 2, 2, 1);

        // Act
        byte[] pdf = service.generatePdf(paginated);

        // Assert
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }


    @Test
    @DisplayName("generatePdf - Debe generar PDF válido con lista vacía (tabla vacía)")
    void generatePdf_ShouldReturnValidPdf_WhenEmptyList() {
        // Arrange
        PaginatedReportSummaryResponse paginated =
                new PaginatedReportSummaryResponse(Collections.emptyList(), 1, 10, 0, 0);

        // Act
        byte[] pdf = service.generatePdf(paginated);

        // Assert
        assertNotNull(pdf, "El PDF no debe ser nulo");
        assertTrue(pdf.length > 0, "El PDF no debe estar vacío incluso si la tabla está vacía");
    }


}
