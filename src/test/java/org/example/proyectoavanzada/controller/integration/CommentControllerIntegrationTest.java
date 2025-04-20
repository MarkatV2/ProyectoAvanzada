package org.example.proyectoavanzada.controller.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.bson.types.ObjectId;
import org.example.proyectoavanzada.util.LoginUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.configuration.SecurityConfig;
import co.edu.uniquindio.proyecto.dto.comment.CommentRequest;
import co.edu.uniquindio.proyecto.dto.comment.CommentResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageUploadRequest;
import co.edu.uniquindio.proyecto.entity.comment.Comment;
import co.edu.uniquindio.proyecto.entity.comment.CommentStatus;
import co.edu.uniquindio.proyecto.entity.image.Image;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.service.interfaces.ImageService;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for the CommentController.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(classes = { ProyectoApplication.class, LoginUtils.class, SecurityConfig.class })
class CommentControllerIntegrationTest {

  @Autowired
  private MongoTemplate mongoTemplate;
  @Autowired
  private TestRestTemplate restTemplate;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private ImageService imageService;
  @MockitoBean
  private EmailService emailService;
  @Autowired
  private PasswordEncoder passwordEncoder;
  @Autowired
  private LoginUtils loginUtils;
  private List<Comment> testComments;

  @BeforeEach
  void setUp() {
    mongoTemplate.getDb().drop();
    mongoTemplate.dropCollection(Comment.class);

    // Crear usuario normal
    User user = new User();
    user.setId(new ObjectId());
    user.setEmail("user@example.com");
    user.setPassword(passwordEncoder.encode("user123"));
    user.setRol(Rol.USER);
    user.setAccountStatus(AccountStatus.ACTIVATED);
    mongoTemplate.insert(user);

    // Inserta 8 reportes con ubicaciones alrededor de (75,4)
    List<Report> reportList = IntStream.rangeClosed(0, 5)
        .mapToObj(i -> {
          double lon = 75 + (i % 2 == 0 ? 0.005 * i : -0.003 * i);
          double lat = 4 + (i % 3 == 0 ? 0.007 * i : -0.004 * i);
          Report r = new Report();
          r.setId(new ObjectId());
          r.setTitle("Reporte " + i);
          r.setDescription("Descripción " + i);
          r.setLocation(new GeoJsonPoint(lon, lat));
          r.setUserId(user.getId());
          r.setUserEmail("user" + i + "@example.com");
          r.setReportStatus(ReportStatus.VERIFIED);
          r.setImportantVotes(i);
          r.setCreatedAt(LocalDateTime.now().minusDays(i));
          return r;
        })
        .toList();

    mongoTemplate.insertAll(reportList);
    testComments = IntStream.range(0, 5)
        .mapToObj(i -> {
          Comment image = new Comment();
          image.setId(new ObjectId());
          image.setReportId(reportList.get(i).getId());
          image.setUserId(user.getId());
          image.setUserName("user123");
          image.setComment("comment" + i);
          image.setCommentStatus(CommentStatus.PUBLISHED);
          return image;
        }).toList();

    mongoTemplate.insertAll(testComments);


  }

  @Test
  void testRegisterCommentIntegration() {
    CommentRequest commentRequest = new CommentRequest(
        "Comment6",
        testComments.get(0).getReportId().toHexString());
    HttpEntity<CommentRequest> request = generateHeader(
        commentRequest);

    // ACT: Send POST request to register a new comment
    ResponseEntity<CommentResponse> response = restTemplate.postForEntity("/api/v1/comments", request,
        CommentResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    List<Comment> comments = mongoTemplate.findAll(Comment.class);
    assertEquals(6, comments.size());
  }

  private <T> HttpEntity<T> generateHeader(T imageUploadRequest) {
    String token = loginUtils.obtenerTokenUsuario();
    HttpHeaders headers = loginUtils.crearHeadersConToken(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(imageUploadRequest, headers);
  }

  @Test
  void testGetCommentByIdIntegration() throws Exception {
    // ARRANGE: Persist a test comment in the database
    String token = loginUtils.obtenerTokenUsuario();
    HttpHeaders headers = loginUtils.crearHeadersConToken(token);

    HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

    // ACT
    ResponseEntity<CommentResponse> response = restTemplate.exchange(
        "/api/v1/comments/" + testComments.get(0).getId().toHexString(),
        HttpMethod.GET, httpEntity,
        CommentResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(testComments.get(0).getId().toString(), response.getBody().id().toString());
    assertEquals(testComments.get(0).getComment(), response.getBody().comment());
  }

  @Test
  void testDeactivateCommentIntegration() throws Exception {
    // ARRANGE: Persist a test image in the database
    String token = loginUtils.obtenerTokenUsuario();
    HttpHeaders headers = loginUtils.crearHeadersConToken(token);

    HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

    ResponseEntity<CommentResponse> response = restTemplate.exchange("/api/v1/comments/" + testComments.get(0).getId().toHexString(),
        HttpMethod.DELETE, httpEntity,
        CommentResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    List<Comment> comments = mongoTemplate.findAll(Comment.class);
    assertEquals(5, comments.size());
    assertEquals(CommentStatus.ELIMINATED, comments.get(0).getCommentStatus());
  }
}