package org.example.proyectoavanzada.controller.integration;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.configuration.SecurityConfig;
import co.edu.uniquindio.proyecto.controller.AuthController;
import co.edu.uniquindio.proyecto.dto.comment.CommentRequest;
import co.edu.uniquindio.proyecto.dto.comment.CommentResponse;
import co.edu.uniquindio.proyecto.entity.category.CategoryRef;
import co.edu.uniquindio.proyecto.entity.comment.Comment;
import co.edu.uniquindio.proyecto.entity.comment.CommentStatus;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import co.edu.uniquindio.proyecto.exceptionhandler.auth.AuthExceptionHandler;
import co.edu.uniquindio.proyecto.exceptionhandler.auth.SecurityErrorHandler;
import co.edu.uniquindio.proyecto.exceptionhandler.global.GlobalExceptionHandler;
import co.edu.uniquindio.proyecto.exceptionhandler.user.UserExceptionHandler;
import co.edu.uniquindio.proyecto.service.EmailService;
import org.bson.types.ObjectId;
import org.example.proyectoavanzada.configuration.TestSecurityConfig;
import org.example.proyectoavanzada.util.LoginUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
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
class CommentControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private EmailService emailService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private LoginUtils loginUtils;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private ObjectId reportIdParaPruebas;

    private String tokenAdmin;
    private String tokenUsuario;

    private ObjectId idCommentParaPruebas;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(User.class);
        mongoTemplate.dropCollection(CategoryRef.class);
        mongoTemplate.dropCollection(Report.class);
        mongoTemplate.dropCollection(Comment.class); // Limpiar colección de comentarios

        // Crear usuarios
        User admin = crearUsuario("admin@example.com", "admin123", "Admin User", Rol.ADMIN, AccountStatus.ACTIVATED);
        User usuario = crearUsuario("user@example.com", "user123", "Regular User", Rol.USER, AccountStatus.ACTIVATED);

        mongoTemplate.save(admin);
        mongoTemplate.save(usuario);

        // Crear categorías
        CategoryRef category1 = new CategoryRef("Category1");
        CategoryRef category2 = new CategoryRef("Category2");

        mongoTemplate.save(category1);
        mongoTemplate.save(category2);

        // Crear reportes
        Report reporte1 = new Report();
        reporte1.setTitle("Fuga de agua");
        reporte1.setDescription("Se detectó una fuga de agua en la calle principal");
        reporte1.setCategoryList(List.of(category1));
        reporte1.setLocation(new GeoJsonPoint(-99.1332, 19.4326));
        reporte1.setUserEmail(usuario.getEmail());
        reporte1.setUserId(usuario.getId());
        reporte1.setReportStatus(ReportStatus.PENDING);
        reporte1.setImportantVotes(3);
        reporte1.setCreatedAt(LocalDateTime.now());

        Report reporte2 = new Report();
        reporte2.setTitle("Alumbrado público dañado");
        reporte2.setDescription("Farolas apagadas durante la noche en el parque");
        reporte2.setCategoryList(List.of(category2));
        reporte2.setLocation(new GeoJsonPoint(-99.1400, 19.4400));
        reporte2.setUserEmail(usuario.getEmail());
        reporte2.setUserId(usuario.getId());
        reporte2.setReportStatus(ReportStatus.RESOLVED);
        reporte2.setImportantVotes(1);
        reporte2.setCreatedAt(LocalDateTime.now());

        mongoTemplate.save(reporte1);
        mongoTemplate.save(reporte2);

        reportIdParaPruebas = reporte1.getId();

        // Crear comentarios
        Comment comment1 = new Comment();
        comment1.setUserName(usuario.getFullName());
        comment1.setUserId(usuario.getId());
        comment1.setReportId(reportIdParaPruebas);
        comment1.setComment("Esto necesita atención urgente.");
        comment1.setCreatedAt(LocalDateTime.now());
        comment1.setCommentStatus(CommentStatus.PUBLISHED);

        Comment comment2 = new Comment();
        comment2.setUserName(admin.getFullName());
        comment2.setUserId(admin.getId());
        comment2.setReportId(reportIdParaPruebas);
        comment2.setComment("Ya se reportó al departamento correspondiente.");
        comment2.setCreatedAt(LocalDateTime.now());
        comment2.setCommentStatus(CommentStatus.PUBLISHED);

        mongoTemplate.save(comment1);
        mongoTemplate.save(comment2);

        idCommentParaPruebas = comment1.getId();

        // Obtener tokens
        tokenAdmin = loginUtils.obtenerTokenAdmin();
        tokenUsuario = loginUtils.obtenerTokenUsuario();
    }


    private User crearUsuario(
            String email,
            String passwordPlano,
            String nombreCompleto,
            Rol rol,
            AccountStatus estado
    ) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(passwordPlano));
        user.setFullName(nombreCompleto);
        user.setDateBirth(LocalDateTime.of(1990, 1, 1, 0, 0));
        user.setCreatedAt(LocalDateTime.now());
        user.setRol(rol);
        user.setAccountStatus(estado);
        user.setCityOfResidence("CiudadX");
        user.setNotificationRadiusKm(5.0);
        // Opcional: no seteamos la ubicación aquí
        return user;
    }


    @Test
    void testCreateComment() {
        // Arrange
        CommentRequest commentRequest = new CommentRequest("Este es un comentario", reportIdParaPruebas.toHexString());

        // Crear los headers con el token de usuario
        HttpHeaders headers = loginUtils.crearHeadersConToken(tokenUsuario);
        HttpEntity<CommentRequest> requestEntity = new HttpEntity<>(commentRequest, headers);

        // Act & Assert
        ResponseEntity<CommentResponse> response = restTemplate.exchange(
                "/api/v1/comments",
                HttpMethod.POST,
                requestEntity,
                CommentResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void testGetCommentById() {
        // Arrange
        String commentId = idCommentParaPruebas.toHexString();

        // Crear los headers con el token de usuario
        HttpHeaders headers = loginUtils.crearHeadersConToken(tokenUsuario);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // Act & Assert
        ResponseEntity<CommentResponse> response = restTemplate.exchange(
                "/api/v1/comments/{commentId}",
                HttpMethod.GET,
                requestEntity,
                CommentResponse.class,
                commentId
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testSoftDeleteComment() {
        // Arrange
        String commentId = idCommentParaPruebas.toHexString();

        // Crear los headers con el token de admin
        HttpHeaders headers = loginUtils.crearHeadersConToken(tokenAdmin);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // Act & Assert
        ResponseEntity<CommentResponse> response = restTemplate.exchange(
                "/api/v1/comments/{commentId}",
                HttpMethod.DELETE,
                requestEntity,
                CommentResponse.class,
                commentId
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testGetCommentById_NotFound() {
        // Arrange
        String nonExistentCommentId = new ObjectId().toHexString();
        HttpHeaders headers = loginUtils.crearHeadersConToken(tokenUsuario);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/comments/{commentId}",
                HttpMethod.GET,
                requestEntity,
                String.class,
                nonExistentCommentId
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }


    @Test
    void testCreateComment_ExceedingLength() {
        // Arrange
        String longComment = "a".repeat(801);
        CommentRequest request = new CommentRequest(longComment, "report123");

        HttpHeaders headers = loginUtils.crearHeadersConToken(tokenUsuario);
        HttpEntity<CommentRequest> requestEntity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/comments",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }


    @Test
    void testCreateComment_InvalidReportId() {
        // Arrange
        CommentRequest request = new CommentRequest("Comentario válido", ""); // reportId vacío

        HttpHeaders headers = loginUtils.crearHeadersConToken(tokenUsuario);
        HttpEntity<CommentRequest> requestEntity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/comments",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }


    @Test
    void testSoftDeleteComment_NotOwnerOrAdmin() {
        // Arrange
        // Crea un comentario con otro usuario (ej. admin), pero intenta borrarlo con un usuario sin permisos

        CommentRequest commentRequest = new CommentRequest("Comentario protegido", reportIdParaPruebas.toHexString());

        // Crear comentario con el ADMIN
        HttpHeaders adminHeaders = loginUtils.crearHeadersConToken(tokenAdmin);
        HttpEntity<CommentRequest> requestEntity = new HttpEntity<>(commentRequest, adminHeaders);

        ResponseEntity<CommentResponse> createResponse = restTemplate.exchange(
                "/api/v1/comments",
                HttpMethod.POST,
                requestEntity,
                CommentResponse.class
        );

        String commentId = createResponse.getBody().id();

        // Act: intentar borrar con un USUARIO que no es dueño
        HttpHeaders userHeaders = loginUtils.crearHeadersConToken(tokenUsuario);
        HttpEntity<Void> deleteRequest = new HttpEntity<>(userHeaders);

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/api/v1/comments/{commentId}",
                HttpMethod.DELETE,
                deleteRequest,
                String.class,
                commentId
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, deleteResponse.getStatusCode());
    }


}
