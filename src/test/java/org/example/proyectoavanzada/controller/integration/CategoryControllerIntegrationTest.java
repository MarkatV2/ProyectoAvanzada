package org.example.proyectoavanzada.controller.integration;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.configuration.SecurityConfig;
import co.edu.uniquindio.proyecto.dto.category.CategoryRequest;
import co.edu.uniquindio.proyecto.dto.category.CategoryResponse;
import co.edu.uniquindio.proyecto.dto.response.ErrorResponse;
import co.edu.uniquindio.proyecto.entity.category.Category;
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
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(classes = {ProyectoApplication.class, LoginUtils.class, SecurityConfig.class})
@Import({SecurityErrorHandler.class})
class CategoryControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private EmailService emailService;

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginUtils loginUtils;

    private List<Category> allCategories;
    private List<Category> activeCategories;

    @BeforeEach
    void setUp() {
        // Limpia la colección
        mongoTemplate.dropCollection(Category.class);
        mongoTemplate.dropCollection(User.class);

        // Crear usuario admin para pruebas
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRol(Rol.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVATED);
        mongoTemplate.insert(admin);


        // Inserta 8 categorías: pares activas, impares inactivas
        allCategories = IntStream.rangeClosed(1, 8)
                .mapToObj(i -> {
                    Category c = new Category();
                    c.setId(new ObjectId());
                    c.setName("Categoría " + i);
                    c.setDescription("Descripción " + i);
                    c.setCreatedAt(LocalDateTime.now());
                    c.setActivated(i % 2 == 0);
                    return c;
                })
                .collect(Collectors.toList());

        mongoTemplate.insertAll(allCategories);
        activeCategories = allCategories.stream()
                .filter(Category::isActivated)
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("GET /api/v1/categories debe devolver todas las activas (ADMIN)")
    void getAllActiveCategories_shouldReturnOnlyActive() {
        // Arrange
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        // Act
        ResponseEntity<CategoryResponse[]> resp = restTemplate.exchange(
                "/api/v1/categories",
                HttpMethod.GET,
                req,
                CategoryResponse[].class
        );

        // Assert
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        CategoryResponse[] body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body).hasSize(activeCategories.size());
        // Por ejemplo, la primera activa debería ser la Categoría 2
        assertThat(body[0].name()).isEqualTo("Categoría 2");
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id} con ID activo devuelve 200 (ADMIN)")
    void getCategoryById_shouldReturn200ForActive() {
        // Arrange
        Category active = activeCategories.get(0);
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        // Act
        ResponseEntity<CategoryResponse> resp = restTemplate.exchange(
                "/api/v1/categories/" + active.getId().toHexString(),
                HttpMethod.GET,
                req,
                CategoryResponse.class
        );

        // Assert
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        CategoryResponse cr = resp.getBody();
        assertThat(cr).isNotNull();
        assertThat(cr.id()).isEqualTo(active.getId().toHexString());
        assertThat(cr.name()).isEqualTo(active.getName());
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id} con ID inactivo devuelve 404 (ADMIN)")
    void getCategoryById_shouldReturn404ForInactive() {
        // Arrange
        Category inactive = allCategories.stream()
                .filter(c -> !c.isActivated())
                .findFirst()
                .orElseThrow();
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        // Act
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/v1/categories/" + inactive.getId().toHexString(),
                HttpMethod.GET,
                req,
                Void.class
        );

        // Assert
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id} con ID inválido devuelve 400 (ADMIN)")
    void getCategoryById_shouldReturn400ForInvalidId() {
        // Arrange
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        // Act
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/v1/categories/invalid-id",
                HttpMethod.GET,
                req,
                Void.class
        );

        // Assert
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/v1/categories - éxito crea categoría (ADMIN)")
    void createCategory_shouldReturn201AndLocationHeader() {
        // Arrange
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);

        CategoryRequest newReq = new CategoryRequest("Nueva Cat", "Descripción nueva");
        HttpEntity<CategoryRequest> req = new HttpEntity<>(newReq, headers);

        // Act
        ResponseEntity<CategoryResponse> resp = restTemplate.exchange(
                "/api/v1/categories",
                HttpMethod.POST,
                req,
                CategoryResponse.class
        );

        // Assert
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // Body no nulo y contiene ID
        CategoryResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotEmpty();
        assertThat(body.name()).isEqualTo("Nueva Cat");

        // Location header apunta al nuevo recurso
        String location = resp.getHeaders().getLocation().toString();
        assertThat(location).endsWith("/api/v1/categories/" + body.id());

        // Además, compruebo que en la BD quedó guardada
        Category saved = mongoTemplate.findById(new ObjectId(body.id()), Category.class);
        assertThat(saved).isNotNull();
        assertThat(saved.getName()).isEqualTo("Nueva Cat");
        assertThat(saved.isActivated()).isTrue(); // por defecto debe venir activada
    }

    @Test
    @DisplayName("POST /api/v1/categories - nombre duplicado retorna 409 (ADMIN)")
    void createCategory_shouldReturn409OnDuplicateName() {
        // Arrange: tomo una categoría ya existente
        Category existing = allCategories.get(0);
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);

        CategoryRequest dupReq = new CategoryRequest(existing.getName(), "Cualquier desc");
        HttpEntity<CategoryRequest> req = new HttpEntity<>(dupReq, headers);

        // Act
        ResponseEntity<ErrorResponse> resp = restTemplate.exchange(
                "/api/v1/categories",
                HttpMethod.POST,
                req,
                ErrorResponse.class
        );

        // Assert
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().message())
                .contains("ya existe"); // según tu ErrorResponseBuilder
    }

    // === UPDATE CATEGORY ===

    @Test
    @DisplayName("PUT /api/v1/categories/{id} - éxito actualiza categoría (ADMIN)")
    void updateCategory_shouldReturn200AndLocationHeader() {
        // Arrange: elijo una activa
        Category toUpdate = activeCategories.get(0);
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);

        CategoryRequest updateReq = new CategoryRequest("Modificada", "Desc modif");
        HttpEntity<CategoryRequest> req = new HttpEntity<>(updateReq, headers);

        // Act
        ResponseEntity<CategoryResponse> resp = restTemplate.exchange(
                "/api/v1/categories/" + toUpdate.getId().toHexString(),
                HttpMethod.PUT,
                req,
                CategoryResponse.class
        );

        // Assert
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        CategoryResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(toUpdate.getId().toHexString());
        assertThat(body.name()).isEqualTo("Modificada");

        // Location header debe apuntar al mismo recurso
        String location = resp.getHeaders().getFirst(HttpHeaders.LOCATION);
        assertThat(location).endsWith("/api/v1/categories/" + toUpdate.getId().toHexString());

        // Verifico en BD
        Category saved = mongoTemplate.findById(toUpdate.getId(), Category.class);
        assertThat(saved.getName()).isEqualTo("Modificada");
    }

    @Test
    @DisplayName("PUT /api/v1/categories/{id} - no encontrada retorna 404 (ADMIN)")
    void updateCategory_shouldReturn404WhenNotFound() {
        // Arrange
        String fakeId = new ObjectId().toHexString();
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);

        CategoryRequest reqBody = new CategoryRequest("X", "Y");
        HttpEntity<CategoryRequest> req = new HttpEntity<>(reqBody, headers);

        // Act
        ResponseEntity<ErrorResponse> resp = restTemplate.exchange(
                "/api/v1/categories/" + fakeId,
                HttpMethod.PUT,
                req,
                ErrorResponse.class
        );

        // Assert
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("PUT /api/v1/categories/{id} - ID inválido retorna 400 (ADMIN)")
    void updateCategory_shouldReturn400OnInvalidId() {
        // Arrange
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);

        CategoryRequest reqBody = new CategoryRequest("X", "Y");
        HttpEntity<CategoryRequest> req = new HttpEntity<>(reqBody, headers);

        // Act
        ResponseEntity<ErrorResponse> resp = restTemplate.exchange(
                "/api/v1/categories/invalid-id",
                HttpMethod.PUT,
                req,
                ErrorResponse.class
        );

        // Assert
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // === DELETE CATEGORY ===

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} - éxito desactiva categoría (ADMIN)")
    void deleteCategory_shouldReturn204AndDeactivate() {
        // Arrange: tomo una categoría activa
        Category toDelete = activeCategories.get(0);
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        // Act
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/v1/categories/" + toDelete.getId().toHexString(),
                HttpMethod.DELETE,
                req,
                Void.class
        );

        // Assert HTTP
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verifico en BD que ahora está desactivada
        Category reloaded = mongoTemplate.findById(toDelete.getId(), Category.class);
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.isActivated()).isFalse();
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} - no encontrada retorna 404 (ADMIN)")
    void deleteCategory_shouldReturn404WhenNotFound() {
        // Arrange
        String fakeId = new ObjectId().toHexString();
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        // Act
        ResponseEntity<ErrorResponse> resp = restTemplate.exchange(
                "/api/v1/categories/" + fakeId,
                HttpMethod.DELETE,
                req,
                ErrorResponse.class
        );

        // Assert
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} - ID inválido retorna 400 (ADMIN)")
    void deleteCategory_shouldReturn400OnInvalidId() {
        // Arrange
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        // Act
        ResponseEntity<ErrorResponse> resp = restTemplate.exchange(
                "/api/v1/categories/invalid-id",
                HttpMethod.DELETE,
                req,
                ErrorResponse.class
        );

        // Assert
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

}

