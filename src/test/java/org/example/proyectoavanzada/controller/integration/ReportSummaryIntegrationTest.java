package org.example.proyectoavanzada.controller.integration;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.configuration.SecurityConfig;
import co.edu.uniquindio.proyecto.dto.report.ReportFilterDTO;
import co.edu.uniquindio.proyecto.entity.category.CategoryRef;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exceptionhandler.auth.SecurityErrorHandler;
import co.edu.uniquindio.proyecto.repository.CategoryRepository;
import co.edu.uniquindio.proyecto.repository.ReportRepository;
import co.edu.uniquindio.proyecto.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.example.proyectoavanzada.util.LoginUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(classes = {ProyectoApplication.class, LoginUtils.class, SecurityConfig.class})
@Import({SecurityErrorHandler.class})
public class ReportSummaryIntegrationTest {


    @Autowired
    private TestRestTemplate restTemplate;
    @MockitoBean
    private EmailService emailService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LoginUtils loginUtils;

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;

    private final List<ObjectId> insertedReportIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mongoTemplate.getDb().drop();
        mongoTemplate.indexOps(Report.class)
                .ensureIndex(new GeospatialIndex("location")
                        .typed(GeoSpatialIndexType.GEO_2DSPHERE));

        // Crear usuario admin para pruebas
        User admin = new User();
        admin.setId(new ObjectId());
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRol(Rol.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVATED);

        mongoTemplate.insert(admin);

        adminToken = loginUtils.obtenerTokenAdmin();

        // Crear categorías
        CategoryRef category1 = new CategoryRef("Basura");
        CategoryRef category2 = new CategoryRef("Ruido");
        CategoryRef category3 = new CategoryRef("Inseguridad");

        // Coordenadas base (Armenia, Quindío)
        GeoJsonPoint baseLocation = new GeoJsonPoint(75.678, 4.535);

        // Crear 8 reportes variados
        List<Report> reports = List.of(
                createReport("Basura en la calle", List.of(category1), ReportStatus.PENDING, baseLocation, LocalDateTime.now().minusDays(1)),
                createReport("Contenedor lleno", List.of(category1), ReportStatus.RESOLVED, baseLocation, LocalDateTime.now().minusDays(5)),
                createReport("Ruido en la noche", List.of(category2), ReportStatus.VERIFIED, baseLocation, LocalDateTime.now().minusDays(10)),
                createReport("Banda sospechosa", List.of(category3), ReportStatus.REJECTED, baseLocation, LocalDateTime.now().minusDays(3)),
                createReport("Ruido en la madrugada", List.of(category2), ReportStatus.DELETED, baseLocation, LocalDateTime.now().minusDays(2)),
                createReport("Basura industrial", List.of(category1), ReportStatus.PENDING, baseLocation, LocalDateTime.now().minusDays(7)),
                createReport("Pelea callejera", List.of(category3), ReportStatus.RESOLVED, baseLocation, LocalDateTime.now().minusDays(4)),
                createReport("Petardos ilegales", List.of(category2), ReportStatus.VERIFIED, baseLocation, LocalDateTime.now().minusDays(6))
        );

        mongoTemplate.insertAll(reports);
        reports.forEach(r -> insertedReportIds.add(r.getId()));
    }

    private Report createReport(String title, List<CategoryRef> categories, ReportStatus status, GeoJsonPoint location, LocalDateTime createdAt) {
        Report report = new Report();
        report.setTitle(title);
        report.setDescription("Descripción de " + title);
        report.setCategoryList(categories);
        report.setReportStatus(status);
        report.setLocation(location);
        report.setUserEmail("test@example.com");
        report.setUserId(new ObjectId());
        report.setCreatedAt(createdAt);
        return report;
    }


    // ------------------------------------------- GENERATE_PDF_REPORT_SUMMARY -------------------------------------------- //


    @DisplayName("🔎 Filtrado por fecha: Debe retornar un PDF con reportes recientes")
    @Test
    void testGeneratePdfWithDateFilter_shouldReturnPdf() {
        ReportFilterDTO filter = new ReportFilterDTO(
                LocalDateTime.now().minusDays(6),
                LocalDateTime.now(),
                null,
                null,
                0
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ReportFilterDTO> request = new HttpEntity<>(filter, headers);

        ResponseEntity<byte[]> response = restTemplate.postForEntity(
                "/api/v1/admin/reportSummaries/pdf?page=1&size=20", request, byte[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }


    @DisplayName("🔎 Filtrado por categoría: Debe retornar un PDF con reportes de 'Basura'")
    @Test
    void testGeneratePdfWithCategoryFilter_shouldReturnPdf() {
        List<String> categoryIds = categoryRepository.findAll().stream()
                .filter(cat -> cat.getName().equals("Basura"))
                .map(c -> c.getId().toString())
                .toList();

        ReportFilterDTO filter = new ReportFilterDTO(null, null, categoryIds, null, 0);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ReportFilterDTO> request = new HttpEntity<>(filter, headers);

        ResponseEntity<byte[]> response = restTemplate.postForEntity(
                "/api/v1/admin/reportSummaries/pdf?page=1&size=20", request, byte[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }

    @DisplayName("📍 Filtrado por ubicación: Debe retornar un PDF con reportes cercanos")
    @Test
    void testGeneratePdfWithLocationFilter_shouldReturnPdf() {
        GeoJsonPoint center = new GeoJsonPoint(75.678, 4.535);

        ReportFilterDTO filter = new ReportFilterDTO(null, null, null, center, 5.0);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ReportFilterDTO> request = new HttpEntity<>(filter, headers);

        ResponseEntity<byte[]> response = restTemplate.postForEntity(
                "/api/v1/admin/reportSummaries/pdf?page=1&size=20", request, byte[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }

    @DisplayName("📊 Filtros combinados: Debe retornar un PDF con filtros de fecha, categoría y ubicación")
    @Test
    void testGeneratePdfWithAllFilters_shouldReturnPdf() {
        List<String> categoryIds = categoryRepository.findAll().stream()
                .map(c -> c.getId().toString())
                .toList();

        GeoJsonPoint center = new GeoJsonPoint(75.678, 4.535);

        ReportFilterDTO filter = new ReportFilterDTO(
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now(),
                categoryIds,
                center,
                10.0
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ReportFilterDTO> request = new HttpEntity<>(filter, headers);

        ResponseEntity<byte[]> response = restTemplate.postForEntity(
                "/api/v1/admin/reportSummaries/pdf?page=1&size=20", request, byte[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }

    @DisplayName("🚫 Sin autorización: Debe retornar estado 401")
    @Test
    void testGeneratePdfWithoutAuthorization_shouldReturnForbidden() {
        ReportFilterDTO filter = new ReportFilterDTO(null, null, null, null, 0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ReportFilterDTO> request = new HttpEntity<>(filter, headers);

        ResponseEntity<byte[]> response = restTemplate.postForEntity(
                "/api/v1/admin/reportSummaries/pdf?page=1&size=20", request, byte[].class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

}
