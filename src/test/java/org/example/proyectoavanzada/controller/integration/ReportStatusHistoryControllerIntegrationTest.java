package org.example.proyectoavanzada.controller.integration;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.configuration.SecurityConfig;
import co.edu.uniquindio.proyecto.dto.report.PaginatedHistoryResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusHistoryResponse;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.entity.report.ReportStatusHistory;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exceptionhandler.auth.SecurityErrorHandler;
import co.edu.uniquindio.proyecto.service.EmailService;
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
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(classes = {ProyectoApplication.class, LoginUtils.class, SecurityConfig.class})
@Import({SecurityErrorHandler.class})
public class ReportStatusHistoryControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @MockitoBean
    private EmailService emailService;

    @Autowired
    private LoginUtils loginUtils;

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private List<ReportStatusHistory> reportHistories;

    private String adminToken;


    @BeforeEach
    void setUp(){
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

        ObjectId reportId1 = new ObjectId();
        ObjectId reportId2 = new ObjectId();

        Report report = new Report();
        Report report1 = new Report();
        report1.setId(reportId1);
        report.setId(reportId2);

        mongoTemplate.insert(report1);
        mongoTemplate.insert(report);

        ObjectId userId1 = new ObjectId();
        ObjectId userId2 = new ObjectId();

        User user = new User();
        User user1 = new User();

        user.setId(userId1);
        user1.setId(userId2);

        mongoTemplate.insert(user1);
        mongoTemplate.insert(user);

        // Crear 8 reportes variados
         reportHistories = List.of(
                createReportHistory(reportId1, userId1, ReportStatus.PENDING, ReportStatus.VERIFIED, LocalDateTime.now().minusDays(1)),
                createReportHistory(reportId1, userId1, ReportStatus.RESOLVED, ReportStatus.DELETED , LocalDateTime.now().minusDays(5)),
                createReportHistory(reportId1, userId2, ReportStatus.VERIFIED, ReportStatus.RESOLVED, LocalDateTime.now().minusDays(10)),
                createReportHistory(reportId1, userId1, ReportStatus.REJECTED, ReportStatus.DELETED , LocalDateTime.now().minusDays(3)),
                createReportHistory(reportId1, userId1, ReportStatus.RESOLVED, ReportStatus.DELETED , LocalDateTime.now().minusDays(2)),
                createReportHistory(reportId2, userId2, ReportStatus.PENDING, ReportStatus.REJECTED , LocalDateTime.now().minusDays(7)),
                createReportHistory(reportId2, userId2, ReportStatus.RESOLVED, ReportStatus.DELETED ,LocalDateTime.now().minusDays(4)),
                createReportHistory(reportId2, userId1, ReportStatus.VERIFIED, ReportStatus.RESOLVED , LocalDateTime.now().minusDays(6))
        );

         mongoTemplate.insertAll(reportHistories);
    }

    private ReportStatusHistory createReportHistory(ObjectId reportId, ObjectId userId, ReportStatus oldStatus,
                                                    ReportStatus newStatus, LocalDateTime changedAt) {
        ReportStatusHistory reportStatusHistory = new ReportStatusHistory();
        reportStatusHistory.setReportId(reportId);
        reportStatusHistory.setUserId(userId);
        reportStatusHistory.setPreviousStatus(oldStatus);
        reportStatusHistory.setNewStatus(newStatus);
        reportStatusHistory.setChangedAt(changedAt);
        return  reportStatusHistory;
    }


    // ------------------------------------------- GET_HISTORY_BY_ID -------------------------------------------- //


    @DisplayName("Obtener historial de cambio de estado de reportes por su id")
    @Test
    void testGetReportStatusHistory_byId_shouldReturnOk() {
        ReportStatusHistory reportHistory = reportHistories.getFirst();
        HttpHeaders headers = loginUtils.crearHeadersConToken(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        String uri = "/api/v1/report-status-histories/" + reportHistory.getId().toHexString();

        ResponseEntity<ReportStatusHistoryResponse> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                request,
                ReportStatusHistoryResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ReportStatusHistoryResponse historyResponse = response.getBody();
        assertNotNull(historyResponse);
        assertEquals(reportHistory.getId().toHexString(), historyResponse.id());
        assertEquals(reportHistory.getUserId().toHexString(), historyResponse.userId());
        assertEquals(reportHistory.getReportId().toHexString(), historyResponse.reportId());
    }

    @DisplayName("Obtener historial con id inexistente")
    @Test
    void testGetReportStatusHistory_byIdNotExisting_shouldReturnNotFound() {
        String historyInexistente = new ObjectId().toHexString();
        HttpHeaders headers = loginUtils.crearHeadersConToken(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        String uri = "/api/v1/report-status-histories/" + historyInexistente;

        ResponseEntity<ReportStatusHistoryResponse> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                request,
                ReportStatusHistoryResponse.class
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }



    // ------------------------------------------- GET_HISTORIES_BY_REPORT -------------------------------------------- //


    @DisplayName("📄 Obtener historial por ID de reporte: debe retornar una lista paginada")
    @Test
    void getHistoryByReportId_shouldReturnPaginatedHistory() {
        String reportId = reportHistories.getFirst().getReportId().toHexString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = String.format("/api/v1/report-status-histories/by-report?reportId=%s&page=1&size=10", reportId);

        ResponseEntity<PaginatedHistoryResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                PaginatedHistoryResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().content().isEmpty());
        assertEquals(reportId, response.getBody().content().get(0).reportId());
    }

    @DisplayName("🚫 Sin token: debe devolver 400 en historial por reporte")
    @Test
    void getHistoryByReportId_withoutToken_shouldReturnForbidden() {

        HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());

        String url = String.format("/api/v1/report-status-histories/by-report?reportId=%s&page=1&size=10",
                reportHistories.getFirst().getReportId().toHexString());

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    // ------------------------------------------- GET_HISTORY_BY_DATE_RANGE -------------------------------------------- //


    @DisplayName("📅 Obtener historial por rango de fechas: debe retornar resultados correctos")
    @Test
    void getHistoryByDateRange_shouldReturnFilteredHistory() {
        String reportId = reportHistories.getFirst().getReportId().toHexString();

        LocalDateTime start = LocalDateTime.now().minusDays(10);
        LocalDateTime end = LocalDateTime.now();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = String.format(
                "/api/v1/report-status-histories/by-report/date-range?reportId=%s&startDate=%s&endDate=%s&page=1&size=10",
                reportId,
                start,
                end
        );

        ResponseEntity<PaginatedHistoryResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                PaginatedHistoryResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().content().isEmpty());

        for (ReportStatusHistoryResponse h : response.getBody().content()) {
            assertTrue(h.changedAt().isAfter(start.minusSeconds(1)));
            assertTrue(h.changedAt().isBefore(end.plusSeconds(1)));
            assertEquals(reportId, h.reportId());
        }
    }


    @DisplayName("🚫 Sin token: debe devolver 400 en historial por rango de fechas")
    @Test
    void getHistoryByDateRange_withoutToken_shouldReturnForbidden() {

        String url = String.format(
                "/api/v1/report-status-histories/by-report/date-range?reportId=%s&startDate=%s&endDate=%s&page=1&size=10",
                reportHistories.getFirst().getReportId().toHexString(),
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now()
        );

        HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    // ------------------------------------------- GET_HISTORY_BY_OLD_STATUS -------------------------------------------- //


    @DisplayName("⏪ Obtener historial por estado anterior: debe retornar historial filtrado correctamente")
    @Test
    void getHistoryByPreviousStatus_shouldReturnFilteredHistory() {
        String reportId = reportHistories.getFirst().getReportId().toHexString();

        // Usamos un estado anterior esperado (PENDING por ejemplo)
        ReportStatus previousStatus = ReportStatus.PENDING;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = String.format(
                "/api/v1/report-status-histories/by-report/previous-status?reportId=%s&previousStatus=%s&page=1&size=10",
                reportId,
                previousStatus.name()
        );

        ResponseEntity<PaginatedHistoryResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                PaginatedHistoryResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        for (ReportStatusHistoryResponse history : response.getBody().content()) {
            assertEquals(previousStatus, history.previousStatus());
            assertEquals(reportId, history.reportId());
        }
    }


    // ------------------------------------------- GET_HISTORY_BY_NEW_STATUS_AND_DATE_RANGE -------------------------------------------- //


    @DisplayName("✅ Obtener historial por estado nuevo y rango de fechas: debe retornar historial filtrado correctamente")
    @Test
    void getHistoryByNewStatusAndDateRange_shouldReturnFilteredHistory() {
        String reportId = reportHistories.getFirst().getReportId().toHexString();

        ReportStatus newStatus = ReportStatus.VERIFIED;
        LocalDateTime start = LocalDateTime.now().minusDays(15);
        LocalDateTime end = LocalDateTime.now();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = String.format(
                "/api/v1/report-status-histories/by-report/new-status-and-dates?reportId=%s&newStatus=%s&startDate=%s&endDate=%s&page=1&size=10",
                reportId,
                newStatus.name(),
                start,
                end
        );

        ResponseEntity<PaginatedHistoryResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                PaginatedHistoryResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        for (ReportStatusHistoryResponse history : response.getBody().content()) {
            assertEquals(newStatus, history.newStatus());
            assertTrue(history.changedAt().isAfter(start.minusSeconds(1)));
            assertTrue(history.changedAt().isBefore(end.plusSeconds(1)));
            assertEquals(reportId, history.reportId());
        }
    }


    // ------------------------------------------- GET_HISTORY_BY_USER -------------------------------------------- //


    @DisplayName("👤 Obtener historial por usuario: debe retornar historial asociado al usuario correctamente")
    @Test
    void getHistoryByUserId_shouldReturnHistoryForUser() {
        ReportStatusHistory anyHistory = mongoTemplate.findAll(ReportStatusHistory.class)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No hay historial en la base de datos"));

        String userId = anyHistory.getUserId().toString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = String.format("/api/v1/report-status-histories/by-user?userId=%s&page=1&size=10", userId);

        ResponseEntity<PaginatedHistoryResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                PaginatedHistoryResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        for (ReportStatusHistoryResponse history : response.getBody().content()) {
            assertEquals(userId, history.userId());
        }
    }


    // ------------------------------------------- COUNT_BY_REPORT -------------------------------------------- //


    @DisplayName("🔢 Contar cambios de estado de un reporte: debe retornar el total correctamente")
    @Test
    void countByReportId_shouldReturnCorrectCount() {

        String reportId = reportHistories.getFirst().getReportId().toHexString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = String.format("/api/v1/report-status-histories/count?reportId=%s", reportId);

        ResponseEntity<Long> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                Long.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody());
    }


}
