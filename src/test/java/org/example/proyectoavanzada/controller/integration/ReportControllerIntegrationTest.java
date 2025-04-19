package org.example.proyectoavanzada.controller.integration;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.configuration.SecurityConfig;
import co.edu.uniquindio.proyecto.controller.AuthController;
import co.edu.uniquindio.proyecto.dto.report.PaginatedReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportRequest;
import co.edu.uniquindio.proyecto.dto.report.ReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportUpdateDto;
import co.edu.uniquindio.proyecto.entity.category.CategoryRef;
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
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
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

}

