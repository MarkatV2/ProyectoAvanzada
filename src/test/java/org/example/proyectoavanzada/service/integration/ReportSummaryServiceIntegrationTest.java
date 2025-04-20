package org.example.proyectoavanzada.service.integration;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.dto.report.PaginatedReportSummaryResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportFilterDTO;
import co.edu.uniquindio.proyecto.entity.category.CategoryRef;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.service.interfaces.ReportSummaryService;
import co.edu.uniquindio.proyecto.service.mapper.ReportSummaryMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(classes = ProyectoApplication.class)
public class ReportSummaryServiceIntegrationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ReportSummaryService reportSummaryService;

    @Autowired
    private ReportSummaryMapper reportSummaryMapper;
    @MockitoBean
    private EmailService emailService;

    private List<Report> testReports;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(Report.class);

        // 2) Asegurar índice 2dsphere en reports.location
        mongoTemplate.indexOps(Report.class)
                .ensureIndex(new GeospatialIndex("location")
                        .typed(GeoSpatialIndexType.GEO_2DSPHERE));

        ObjectId catA = new ObjectId();
        ObjectId catB = new ObjectId();

        testReports = List.of(
                createReport("Reporte 1", new GeoJsonPoint(75, 4), catA, LocalDateTime.now().minusDays(10)),
                createReport("Reporte 2", new GeoJsonPoint(75, 4.01), catA, LocalDateTime.now().minusDays(5)),
                createReport("Reporte 3", new GeoJsonPoint(75.0001, 4.0002), catB, LocalDateTime.now().minusDays(3)),
                createReport("Reporte 4", new GeoJsonPoint(75.0005, 4.0005), catB, LocalDateTime.now().minusDays(2)),
                createReport("Reporte 5", new GeoJsonPoint(75.0001, 4.1), catA, LocalDateTime.now().minusDays(1)),
                createReport("Reporte 6", new GeoJsonPoint(74.95, 4.02), catB, LocalDateTime.now().minusDays(6)),
                createReport("Reporte 7", new GeoJsonPoint(74.96, 4.03), catA, LocalDateTime.now().minusDays(7)),
                createReport("Reporte 8", new GeoJsonPoint(75, 4.02), catA, LocalDateTime.now().minusDays(4))
        );
        mongoTemplate.insertAll(testReports);
    }

    private Report createReport(String title, GeoJsonPoint point, ObjectId categoryId, LocalDateTime createdAt) {
        Report report = new Report();
        report.setTitle(title);
        report.setDescription("Descripción de " + title);
        report.setLocation(point);
        report.setUserEmail("user@example.com");
        report.setUserId(new ObjectId());
        report.setReportStatus(ReportStatus.PENDING);
        report.setImportantVotes(0);
        report.setCreatedAt(createdAt);

        CategoryRef categoryRef = new CategoryRef();
        categoryRef.setId(categoryId.toHexString());
        categoryRef.setName("Categoría de prueba");
        report.setCategoryList(List.of(categoryRef));

        return report;
    }


    // ------------------------------------------- GET_REPORT_WITH_FILTERS -------------------------------------------- //

    @Test
    @DisplayName("Filtrar por rango de fechas devuelve los reportes esperados")
    void whenFilterByDateRange_thenReturnExpectedReports() {
        ReportFilterDTO filter = new ReportFilterDTO(
                LocalDateTime.now().minusDays(6),
                LocalDateTime.now(),
                null,
                null,
                0
        );

        PaginatedReportSummaryResponse response = reportSummaryService.getFilteredReports(filter, 1, 10);

        assertEquals(5, response.content().size());
    }

    @Test
    @DisplayName("Filtrar por categoría devuelve los reportes correspondientes")
    void whenFilterByCategory_thenReturnMatchingReports() {
        String categoryId = testReports.get(0).getCategoryList().get(0).getId();
        ReportFilterDTO filter = new ReportFilterDTO(
                null,
                null,
                List.of(categoryId),
                null,
                0
        );

        PaginatedReportSummaryResponse response = reportSummaryService.getFilteredReports(filter, 1, 10);

        assertEquals(5, response.content().size());
    }

    @Test
    @DisplayName("Filtrar por ubicación devuelve los reportes dentro del radio")
    void whenFilterByLocation_thenReturnReportsInRadius() {
        GeoJsonPoint center = new GeoJsonPoint(75, 4);
        ReportFilterDTO filter = new ReportFilterDTO(
                null,
                null,
                null,
                center,
                10.0 // km
        );

        PaginatedReportSummaryResponse response = reportSummaryService.getFilteredReports(filter, 1, 10);

        assertTrue(response.content().size() >= 4); // mínimo 4 están dentro del rango
    }

    @Test
    @DisplayName("Filtrar con todos los criterios combinados devuelve resultados correctos")
    void whenFilterByAllCriteria_thenReturnExpectedResults() {
        String categoryId = testReports.get(0).getCategoryList().get(0).getId();
        GeoJsonPoint center = new GeoJsonPoint(75, 4);

        ReportFilterDTO filter = new ReportFilterDTO(
                LocalDateTime.now().minusDays(6),
                LocalDateTime.now(),
                List.of(categoryId),
                center,
                5.0
        );

        PaginatedReportSummaryResponse response = reportSummaryService.getFilteredReports(filter, 1, 10);

        assertEquals(2, response.content().size());
    }


    // ------------------------------------------- GENERATE_PDF -------------------------------------------- //


    @Test
    @DisplayName("Genera un PDF con todos los reportes")
    void testGeneratePdfFromAllReports() {
        // Sin filtros
        ReportFilterDTO emptyFilter = new ReportFilterDTO(null, null, null, null, 0);
        PaginatedReportSummaryResponse paginated = reportSummaryService.getFilteredReports(emptyFilter, 1, 10);

        assertFalse(paginated.content().isEmpty(), "Debe haber reportes en la base de datos");

        byte[] pdfBytes = reportSummaryService.generatePdf(paginated);

        assertNotNull(pdfBytes, "El PDF generado no debe ser null");
        assertTrue(pdfBytes.length > 0, "El PDF debe tener contenido");
    }

}
