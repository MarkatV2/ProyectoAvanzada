package org.example.proyectoavanzada.controller.integration;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.configuration.SecurityConfig;
import co.edu.uniquindio.proyecto.dto.comment.CommentPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.report.PaginatedReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportRequest;
import co.edu.uniquindio.proyecto.dto.report.ReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportUpdateDto;
import co.edu.uniquindio.proyecto.entity.category.CategoryRef;
import co.edu.uniquindio.proyecto.entity.comment.Comment;
import co.edu.uniquindio.proyecto.entity.comment.CommentStatus;
import co.edu.uniquindio.proyecto.entity.image.Image;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exceptionhandler.auth.SecurityErrorHandler;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(classes = {ProyectoApplication.class, LoginUtils.class, SecurityConfig.class})
@Import({SecurityErrorHandler.class})
class ReportControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private EmailService emailService;
    @Autowired
    private LoginUtils loginUtils;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private List<Report> testReports;

    @BeforeEach
    void setUp() {
        mongoTemplate.getDb().drop();

        // Limpia y prepara colección
        mongoTemplate.dropCollection(Report.class);
        mongoTemplate.indexOps(Report.class)
                .ensureIndex(new GeospatialIndex("location")
                        .typed(GeoSpatialIndexType.GEO_2DSPHERE));

        CategoryRef cat = new CategoryRef();
        cat.setId(new ObjectId().toHexString());      // <-- esto es clave
        cat.setName("Categoría X");

        // Crear usuario admin para pruebas
        User admin = new User();
        admin.setId(new ObjectId());
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRol(Rol.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVATED);

        // Crear usuario normal
        User user = new User();
        user.setId(new ObjectId());
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("user123"));
        user.setRol(Rol.USER);
        user.setAccountStatus(AccountStatus.ACTIVATED);

        User user2 = new User();
        user2.setEmail("other@example.com");
        user2.setPassword(passwordEncoder.encode("other123"));
        user2.setRol(Rol.USER);
        user2.setAccountStatus(AccountStatus.ACTIVATED);

        // Inserta 8 reportes con ubicaciones alrededor de (75,4)
        testReports = IntStream.rangeClosed(1, 8)
                .mapToObj(i -> {
                    double lon = 75 + (i % 2 == 0 ? 0.005 * i : -0.003 * i);
                    double lat = 4 + (i % 3 == 0 ? 0.007 * i : -0.004 * i);
                    Report r = new Report();
                    r.setId(new ObjectId());
                    r.setTitle("Reporte " + i);
                    r.setDescription("Descripción " + i);
                    r.setCategoryList(List.of(cat));
                    r.setLocation(new GeoJsonPoint(lon, lat));
                    r.setUserEmail("user" + i + "@example.com");
                    r.setUserId(user.getId());
                    r.setReportStatus(ReportStatus.VERIFIED);
                    r.setImportantVotes(i);
                    r.setCreatedAt(LocalDateTime.now().minusDays(i));
                    return r;
                })
                .toList();

        mongoTemplate.insertAll(testReports);
        mongoTemplate.insert(admin);
        mongoTemplate.insert(user);
        mongoTemplate.insert(user2);
    }


    // ------------------------------------------- GET_ALL_REPORTS_WITH_FILTERS -------------------------------------------- //


    @Test
    @DisplayName("Consulta reportes cercanos con token válido devuelve 200 y list paginada")
    void givenValidToken_whenFilterReports_thenReturnsOkAndPaginatedResults() {
        // Arrange
        String token = loginUtils.obtenerTokenUsuario();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        String url = "/api/v1/reports?latitud=4&longitud=75&radio=5&page=1&size=5";
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Act
        ResponseEntity<PaginatedReportResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<PaginatedReportResponse>() {}
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        PaginatedReportResponse body = response.getBody();
        assertNotNull(body);
        assertFalse(body.content().isEmpty(), "Debe retornar al menos 1 reporte dentro de 5 km");
        assertEquals(1, body.page());
        assertEquals(5, body.size());
        assertTrue(body.totalElements() >= body.content().size());
    }

    @Test
    @DisplayName("Consulta reportes cercanos filtrando por categorías devuelve sólo esa categoría")
    void givenValidToken_whenFilterReportsByCategory_thenReturnsOnlyThatCategory() {
        // Arrange
        // 1) Obtener token y headers
        String token   = loginUtils.obtenerTokenUsuario();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);

        // 2) Elegimos la primera categoría de uno de los reportes de prueba
        String categoryId = testReports.get(0)
                .getCategoryList().get(0)
                .getId();

        // 3) Formamos la URL con parámetro categories
        String url = String.format(
                "/api/v1/reports?latitud=4&longitud=75&radio=10&categories=%s&page=1&size=10",
                categoryId
        );
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Act
        ResponseEntity<PaginatedReportResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<PaginatedReportResponse>() {}
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        PaginatedReportResponse body = response.getBody();
        assertNotNull(body);

        // Todos los reportes de la página deben incluir la categoría filtrada
        assertTrue(
                body.content().stream()
                        .allMatch(r -> r.categoryList().stream()
                                .anyMatch(c -> c.getId().equals(categoryId))
                        ),
                "Cada reporte devuelto debe pertenecer a la categoría " + categoryId
        );
    }


    @Test
    @DisplayName("Consulta reportes cercanos sin token devuelve 401 Unauthorized")
    void whenFilterReportsWithoutToken_thenReturnsUnauthorized() {
        // Arrange
        String url = "/api/v1/reports?latitud=75&longitud=4";
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    // ------------------------------------------- GET_REPORT_BY_ID -------------------------------------------- //


    @Test
    @DisplayName("Obtener reporte por ID válido con token devuelve 200 y reporte correcto")
    void givenValidToken_whenGetReportById_thenReturnsReport() {
        // Arrange
        Report sample = testReports.get(0);
        String token = loginUtils.obtenerTokenUsuario();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        String url = "/api/v1/reports/" + sample.getId().toHexString();

        // Act
        ResponseEntity<ReportResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                ReportResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ReportResponse report = response.getBody();
        assertNotNull(report);
        assertEquals(sample.getId().toHexString(), report.id());
        assertEquals(sample.getTitle(), report.title());
        assertEquals(sample.getDescription(), report.description());
    }

    @Test
    @DisplayName("Obtener reporte por ID inexistente con token válido devuelve 404 Not Found")
    void givenValidToken_whenGetReportByInvalidId_thenReturnsNotFound() {
        // Arrange
        String fakeId = new ObjectId().toHexString();
        String token = loginUtils.obtenerTokenUsuario();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        String url = "/api/v1/reports/" + fakeId;

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }


    // ------------------------------------------- CREATE_REPORT -------------------------------------------- //


    @Test
    @DisplayName("Crear reporte válido devuelve 201 Created, Location y body correcto")
    void givenValidReportRequest_whenCreateReport_thenReturnsCreated() {
        // Arrange
        String token = loginUtils.obtenerTokenUsuario();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ReportRequest newReport = new ReportRequest(
                "Reporte Nuevo",
                "Esta es la descripción del reporte nuevo",
                List.of(new CategoryRef("CatA")),
                75.001, 4.002
        );

        HttpEntity<ReportRequest> request = new HttpEntity<>(newReport, headers);

        // Act
        ResponseEntity<ReportResponse> response = restTemplate.postForEntity(
                "/api/v1/reports", request, ReportResponse.class);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        URI location = response.getHeaders().getLocation();
        assertNotNull(location, "Debe incluirse header Location");
        assertTrue(location.getPath().contains("/api/v1/reports/"));

        ReportResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Reporte Nuevo", body.title());
        assertEquals("Esta es la descripción del reporte nuevo", body.description());
        assertEquals("CatA",
                body.categoryList().get(0).getName());
        assertEquals(75.001, body.latitude());
        assertEquals(4.002, body.longitude());
    }

    @Test
    @DisplayName("Crear reporte duplicado devuelve 409 Conflict")
    void givenDuplicateReportRequest_whenCreateReport_thenReturnsConflict() {
        // Arrange
        Report existing = testReports.get(0);
        String token = loginUtils.obtenerTokenUsuario();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ReportRequest dupReq = new ReportRequest(
                existing.getTitle(),
                existing.getDescription(),
                List.of(new CategoryRef("CatA")),
                existing.getLocation().getX(),
                existing.getLocation().getY()
        );

        HttpEntity<ReportRequest> request = new HttpEntity<>(dupReq, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/reports", request, String.class);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().contains("ya existe"));
    }

    @Test
    @DisplayName("Crear reporte con datos inválidos devuelve 400 Bad Request")
    void givenInvalidReportRequest_whenCreateReport_thenReturnsBadRequest() {
        // Arrange
        String token = loginUtils.obtenerTokenUsuario();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ReportRequest invalidReq = new ReportRequest(
                "",                         // título vacío
                "Descripción breve",
                List.of(new CategoryRef("Cas")),
                75.0, 4.0
        );
        HttpEntity<ReportRequest> request = new HttpEntity<>(invalidReq, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/reports", request, String.class);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("El titulo es obligatorio"));
    }

    @Test
    @DisplayName("Crear reporte sin token devuelve 401 Unauthorized")
    void givenNoToken_whenCreateReport_thenReturnsUnauthorized() {
        // Arrange
        ReportRequest newReport = new ReportRequest(
                "Reporte Sin Token",
                "Descripción irrelevante",
                List.of(new CategoryRef("Cas")),
                75.0, 4.0
        );
        HttpEntity<ReportRequest> request = new HttpEntity<>(newReport);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/reports", request, String.class);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    // ------------------------------------------- UPDATE_REPORT -------------------------------------------- //


    @Test
    @DisplayName("Actualización de reporte por propietario devuelve 200 y body actualizado")
    void givenOwnerToken_whenUpdateReport_thenReturnsOkAndUpdatedReport() {
        // Arrange
        // Usamos un reporte con índice 0 que en el setup tiene userEmail="user@example.com"
        Report owned = testReports.get(0);
        String reportId = owned.getId().toHexString();

        String token = loginUtils.obtenerTokenUsuario();  // token de user@example.com
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ReportUpdateDto update = new ReportUpdateDto(
                "Título Modificado",
                "Descripción modificada",
                List.of(new CategoryRef("CatB"))
        );
        HttpEntity<ReportUpdateDto> request = new HttpEntity<>(update, headers);

        // Act
        ResponseEntity<ReportResponse> response = restTemplate.exchange(
                "/api/v1/reports/" + reportId,
                HttpMethod.PUT,
                request,
                ReportResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ReportResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(reportId, body.id());
        assertEquals("Título Modificado", body.title());
        assertEquals("Descripción modificada", body.description());
    }

    @Test
    @DisplayName("Actualización de reporte por admin devuelve 200 y body actualizado")
    void givenAdminToken_whenUpdateReport_thenReturnsOkAndUpdatedReport() {
        // Arrange
        Report anyReport = testReports.get(1);
        String reportId = anyReport.getId().toHexString();

        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ReportUpdateDto update = new ReportUpdateDto(
                "Título Admin",
                "Descripción admin",
                List.of(new CategoryRef("CatB"))
        );
        HttpEntity<ReportUpdateDto> request = new HttpEntity<>(update, headers);

        // Act
        ResponseEntity<ReportResponse> response = restTemplate.exchange(
                "/api/v1/reports/" + reportId,
                HttpMethod.PUT,
                request,
                ReportResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ReportResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Título Admin", body.title());
    }

    @Test
    @DisplayName("Actualización de reporte por no propietario devuelve 403 Forbidden")
    void givenOtherUserToken_whenUpdateReport_thenReturnsForbidden() {
        // Arrange
        // testReports.get(2) pertenece a user2@example.com
        Report other = testReports.get(2);
        String reportId = other.getId().toHexString();

        String token = loginUtils.obtenerTokenUsuario("other@example.com", "other123");
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ReportUpdateDto update = new ReportUpdateDto(
                "Intento No Propietario",
                "No debería poder modificar",
                List.of(new CategoryRef("CatB"))
        );
        HttpEntity<ReportUpdateDto> request = new HttpEntity<>(update, headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/reports/" + reportId,
                HttpMethod.PUT,
                request,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("Actualización con datos inválidos devuelve 400 Bad Request")
    void givenInvalidUpdateDto_whenUpdateReport_thenReturnsBadRequest() {
        // Arrange
        Report owned = testReports.get(0);
        String reportId = owned.getId().toHexString();

        String token = loginUtils.obtenerTokenUsuario();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // DTO inválido: título vacío
        ReportUpdateDto invalid = new ReportUpdateDto("", "Desc", List.of());
        HttpEntity<ReportUpdateDto> request = new HttpEntity<>(invalid, headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/reports/" + reportId,
                HttpMethod.PUT,
                request,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("El titulo es obligatorio"));
    }

    @Test
    @DisplayName("Actualización de reporte inexistente devuelve 404 Not Found")
    void givenAdminToken_whenUpdateNonexistentReport_thenReturnsNotFound() {
        // Arrange
        String fakeId = new ObjectId().toHexString();
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ReportUpdateDto update = new ReportUpdateDto(
                "Título Cualquiera",
                "Descripción",
                List.of(new CategoryRef("CatB"))
        );
        HttpEntity<ReportUpdateDto> request = new HttpEntity<>(update, headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/reports/" + fakeId,
                HttpMethod.PUT,
                request,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }


    // ------------------------------------------- DELETE_REPORT -------------------------------------------- //


    @Test
    @DisplayName("El propietario puede eliminar un reporte y luego no se encuentra")
    void givenOwnerToken_whenDeleteReport_thenReturnsNoContentAndReportIsGone() {
        // Arrange
        Report owned = testReports.get(0);
        String reportId = owned.getId().toHexString();
        String token = loginUtils.obtenerTokenUsuario();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Act
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/v1/reports/" + reportId,
                HttpMethod.DELETE,
                request,
                Void.class
        );

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        // Al volver a solicitarlo, debe 404 Not Found
        ResponseEntity<String> getResponse = restTemplate.exchange(
                "/api/v1/reports/" + reportId,
                HttpMethod.GET,
                request,
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    @Test
    @DisplayName("Admin puede eliminar un reporte y obtener 204 No Content")
    void givenAdminToken_whenDeleteReport_thenReturnsNoContent() {
        // Arrange
        Report anyReport = testReports.get(1);
        String reportId = anyReport.getId().toHexString();
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Act
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/reports/" + reportId,
                HttpMethod.DELETE,
                request,
                Void.class
        );

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    @DisplayName("No propietario ni admin al eliminar recibe 403 Forbidden")
    void givenOtherUserToken_whenDeleteReport_thenReturnsForbidden() {
        // Arrange
        Report other = testReports.get(2);
        String reportId = other.getId().toHexString();
        // supongamos que existe otro usuario distinto a user@example.com
        String token = loginUtils.obtenerTokenUsuario("other@example.com","other123");
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/reports/" + reportId,
                HttpMethod.DELETE,
                request,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("Eliminar reporte inexistente devuelve 404 Not Found")
    void givenAdminToken_whenDeleteNonexistentReport_thenReturnsNotFound() {
        // Arrange
        String fakeId = new ObjectId().toHexString();
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/reports/" + fakeId,
                HttpMethod.DELETE,
                request,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }


    // ------------------------------------------- UPDATE_REPORT_STATUS -------------------------------------------- //


    @Test
    @DisplayName("Administrador puede cambiar estado a VERIFIED y retorna 200")
    void givenAdminToken_whenPatchStatusToVerified_thenReturnsOkAndStatusUpdated() {
        // Arrange
        Report target = testReports.get(0);
        String reportId = target.getId().toHexString();
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
        {"status":"VERIFIED","rejectionMessage":null}
        """;
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        // Act
        ResponseEntity<Void> patchResp = restTemplate.exchange(
                "/api/v1/reports/" + reportId + "/status",
                HttpMethod.PATCH,
                request,
                Void.class
        );

        // Assert
        assertEquals(HttpStatus.OK, patchResp.getStatusCode());

        // Corroborar en la consulta GET
        ResponseEntity<ReportResponse> getResp = restTemplate.exchange(
                "/api/v1/reports/" + reportId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ReportResponse.class
        );
        assertEquals("VERIFIED", getResp.getBody().reportStatus());
    }

    @Test
    @DisplayName("Administrador puede REJECTED con mensaje y retorna 200")
    void givenAdminToken_whenPatchStatusToRejectedWithMessage_thenReturnsOk() {
        // Arrange
        Report target = testReports.get(1);
        String reportId = target.getId().toHexString();
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
        {"status":"REJECTED","rejectionMessage":"Datos inválidos"}
        """;
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        // Act
        ResponseEntity<Void> patchResp = restTemplate.exchange(
                "/api/v1/reports/" + reportId + "/status",
                HttpMethod.PATCH,
                request,
                Void.class
        );

        // Assert
        assertEquals(HttpStatus.OK, patchResp.getStatusCode());

        ResponseEntity<ReportResponse> getResp = restTemplate.exchange(
                "/api/v1/reports/" + reportId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ReportResponse.class
        );
        assertEquals("REJECTED", getResp.getBody().reportStatus());
    }

    @Test
    @DisplayName("Rechazo sin mensaje obliga a 400 Bad Request")
    void givenAdminToken_whenPatchStatusToRejectedWithoutMessage_thenReturnsBadRequest() {
        // Arrange
        Report target = testReports.get(2);
        String reportId = target.getId().toHexString();
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
        {"status":"REJECTED","rejectionMessage":""}
        """;
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        // Act
        ResponseEntity<String> patchResp = restTemplate.exchange(
                "/api/v1/reports/" + reportId + "/status",
                HttpMethod.PATCH,
                request,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, patchResp.getStatusCode());
        assertTrue(patchResp.getBody().contains("Debe proporcionar un mensaje de rechazo"));
    }

    @Test
    @DisplayName("Propietario puede marcar RESOLVED y retorna 200")
    void givenOwnerToken_whenPatchStatusToResolved_thenReturnsOk() {
        // Arrange
        Report target = testReports.get(3);
        // el reporte 3 fue creado con userId == user.getId()
        String reportId = target.getId().toHexString();
        String token = loginUtils.obtenerTokenUsuario();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
        {"status":"RESOLVED","rejectionMessage":null}
        """;
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        // Act
        ResponseEntity<Void> patchResp = restTemplate.exchange(
                "/api/v1/reports/" + reportId + "/status",
                HttpMethod.PATCH,
                request,
                Void.class
        );

        // Assert
        assertEquals(HttpStatus.OK, patchResp.getStatusCode());
        ResponseEntity<ReportResponse> getResp = restTemplate.exchange(
                "/api/v1/reports/" + reportId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ReportResponse.class
        );
        assertEquals("RESOLVED", getResp.getBody().reportStatus());
    }

    @Test
    @DisplayName("No propietario ni admin al cambiar a RESOLVED recibe 403 Forbidden")
    void givenOtherUserToken_whenPatchStatusToResolved_thenReturnsForbidden() {
        // Arrange
        Report target = testReports.get(4);
        String reportId = target.getId().toHexString();
        // Este usuario NO es ni owner ni admin
        String token = loginUtils.obtenerTokenUsuario("other@example.com","other123");
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
        {"status":"RESOLVED","rejectionMessage":null}
        """;
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        // Act
        ResponseEntity<String> patchResp = restTemplate.exchange(
                "/api/v1/reports/" + reportId + "/status",
                HttpMethod.PATCH,
                request,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, patchResp.getStatusCode());
    }

    @Test
    @DisplayName("Cambiar estado de reporte inexistente devuelve 404 Not Found")
    void givenAdminToken_whenPatchStatusNonexistentId_thenReturnsNotFound() {
        // Arrange
        String fakeId = new ObjectId().toHexString();
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
        {"status":"VERIFIED","rejectionMessage":null}
        """;
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        // Act
        ResponseEntity<String> patchResp = restTemplate.exchange(
                "/api/v1/reports/" + fakeId + "/status",
                HttpMethod.PATCH,
                request,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, patchResp.getStatusCode());
    }


    // ------------------------------------------- TOGGLE_VOTE -------------------------------------------- //


    @Test
    @DisplayName("Usuario puede alternar su voto importante en un reporte existente")
    void givenUserToken_whenToggleVote_thenVoteAddedOrRemoved() {
        // Arrange
        Report target = testReports.get(0);
        String reportId = target.getId().toHexString();
        String token = loginUtils.obtenerTokenUsuario(); // mismo que creó el reporte 0
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);

        // Obtener votos antes
        ResponseEntity<ReportResponse> beforeResp = restTemplate.exchange(
                "/api/v1/reports/" + reportId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ReportResponse.class
        );
        int votosAntes = beforeResp.getBody().importantVotes();

        // Act
        ResponseEntity<Void> toggleResp = restTemplate.exchange(
                "/api/v1/reports/" + reportId + "/votes",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                Void.class
        );

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, toggleResp.getStatusCode());

        // Verificar que el voto se haya agregado o quitado
        ResponseEntity<ReportResponse> afterResp = restTemplate.exchange(
                "/api/v1/reports/" + reportId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ReportResponse.class
        );
        int votosDespues = afterResp.getBody().importantVotes();

        assertNotEquals(votosAntes, votosDespues);
    }

    @Test
    @DisplayName("Alternar voto en reporte inexistente retorna 404 Not Found")
    void givenUserToken_whenToggleVoteNonExistentReport_thenReturnsNotFound() {
        // Arrange
        String fakeId = new ObjectId().toHexString();
        String token = loginUtils.obtenerTokenUsuario();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);

        // Act
        ResponseEntity<String> toggleResp = restTemplate.exchange(
                "/api/v1/reports/" + fakeId + "/votes",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                String.class
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, toggleResp.getStatusCode());
    }

    @Test
    @DisplayName("Acceso sin token retorna 401 Unauthorized")
    void givenNoToken_whenToggleVote_thenReturnsUnauthorized() {
        // Arrange
        Report target = testReports.get(1);
        String reportId = target.getId().toHexString();

        // Act
        ResponseEntity<String> toggleResp = restTemplate.exchange(
                "/api/v1/reports/" + reportId + "/votes",
                HttpMethod.PATCH,
                HttpEntity.EMPTY,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, toggleResp.getStatusCode());
    }


    // ------------------------------------------- GET_ALL_IMAGES_BY_REPORT -------------------------------------------- //


    @Test
    @DisplayName("Obtener todas las imágenes asociadas a un reporte existente")
    void givenValidReportId_whenGetImages_thenReturnsListOfImages() {
        Report report = testReports.get(0);
        ObjectId reportId = report.getId();

        List<Image> imagenes = IntStream.range(0, 5)
                .mapToObj(i -> {
                    Image img = new Image();
                    img.setId(new ObjectId());
                    img.setImageUrl("https://example.com/imagen" + i + ".jpg");
                    img.setUploadDate(LocalDateTime.now().minusHours(i));
                    img.setReportId(reportId);
                    return img;
                }).toList();

        mongoTemplate.insertAll(imagenes);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginUtils.obtenerTokenUsuario());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<List<ImageResponse>> response = restTemplate.exchange(
                "/api/v1/reports/" + reportId.toHexString() + "/images",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().size());
        assertTrue(response.getBody().stream()
                .allMatch(img -> img.imageUrl().startsWith("https://example.com/")));
    }

    @Test
    @DisplayName("Obtener imágenes de reporte sin imágenes devuelve lista vacía")
    void givenValidReportIdWithoutImages_whenGetImages_thenReturnsEmptyList() {
        Report report = testReports.get(1);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginUtils.obtenerTokenUsuario());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<List<ImageResponse>> response = restTemplate.exchange(
                "/api/v1/reports/" + report.getId().toHexString() + "/images",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("Obtener imágenes con ID inválido devuelve 400 Bad Request")
    void givenInvalidReportId_whenGetImages_thenReturnsBadRequest() {
        String invalidId = "abc123";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginUtils.obtenerTokenUsuario());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/reports/" + invalidId + "/images",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Obtener imágenes de reporte inexistente devuelve 404 Not Found")
    void givenNonExistentReportId_whenGetImages_thenReturnsNotFound() {
        String fakeId = new ObjectId().toHexString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginUtils.obtenerTokenUsuario());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/reports/" + fakeId + "/images",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }


    // ------------------------------------------- GET_ALL_COMMENTS_BY_REPORT -------------------------------------------- //


    @Test
    @DisplayName("Obtener todos los comentarios de un reporte paginados correctamente")
    void givenReportWithComments_whenGetComments_thenReturnsPaginatedList() {
        // Insertar comentarios
        List<Comment> comentarios = IntStream.range(1, 6)
                .mapToObj(i -> {
                    Comment c = new Comment();
                    c.setId(new ObjectId());
                    c.setUserId(testReports.get(0).getId());
                    c.setUserName("Usuario " + i);
                    c.setReportId(testReports.get(0).getId());
                    c.setComment("Este es el comentario número " + i);
                    c.setCreatedAt(LocalDateTime.now().minusMinutes(i));
                    c.setCommentStatus(CommentStatus.PUBLISHED);
                    return c;
                }).toList();

        mongoTemplate.insertAll(comentarios);
        Report report = testReports.get(0);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginUtils.obtenerTokenUsuario());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<CommentPaginatedResponse> response = restTemplate.exchange(
                "/api/v1/reports/" + report.getId().toHexString() + "/comments?page=0&size=5",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        CommentPaginatedResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(5, body.content().size());
        assertEquals(0, body.page());
        assertEquals(5, body.size());
        assertEquals(5, body.totalElements());
        assertEquals(1, body.totalPages());
    }

    @Test
    @DisplayName("Obtener comentarios de reporte válido pero sin comentarios")
    void givenReportWithoutComments_whenGetComments_thenReturnsEmptyPage() {
        Report report = testReports.get(1);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginUtils.obtenerTokenUsuario());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<CommentPaginatedResponse> response = restTemplate.exchange(
                "/api/v1/reports/" + report.getId().toHexString() + "/comments?page=0&size=5",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        CommentPaginatedResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(0, body.content().size());
        assertEquals(0, body.totalElements());
        assertEquals(0, body.totalPages());
    }

    @Test
    @DisplayName("Obtener comentarios con ID inválido devuelve 400")
    void givenInvalidReportId_whenGetComments_thenReturnsBadRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginUtils.obtenerTokenUsuario());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/reports/invalid-id/comments",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Obtener comentarios de reporte inexistente devuelve 404")
    void givenNonExistentReportId_whenGetComments_thenReturnsNotFound() {
        String fakeId = new ObjectId().toHexString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginUtils.obtenerTokenUsuario());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/reports/" + fakeId + "/comments",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }


}

