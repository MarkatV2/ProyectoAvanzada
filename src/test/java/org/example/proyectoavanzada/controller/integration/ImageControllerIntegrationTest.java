package co.edu.uniquindio.proyecto.controller;

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
import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageUploadRequest;
import co.edu.uniquindio.proyecto.entity.image.Image;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.service.interfaces.ImageService;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for the ImageController.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(classes = { ProyectoApplication.class, LoginUtils.class, SecurityConfig.class })
class ImageControllerIntegrationTest {

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
  private List<Image> testImages;

  @BeforeEach
  void setUp() {
    mongoTemplate.getDb().drop();
    mongoTemplate.dropCollection(Image.class);

    testImages = IntStream.range(0, 5)
        .mapToObj(i -> {
          Image image = new Image();
          image.setId(new ObjectId());
          image.setImageUrl("https://res.cloudinary.com/v1/image/upload/" + "testImage" + i + ".jpg");
          image.setReportId(new ObjectId());
          image.setUploadDate(LocalDateTime.now());
          return image;
        }).toList();

    mongoTemplate.insertAll(testImages);
    // Crear usuario normal
    User user = new User();
    user.setId(new ObjectId());
    user.setEmail("user@example.com");
    user.setPassword(passwordEncoder.encode("user123"));
    user.setRol(Rol.USER);
    user.setAccountStatus(AccountStatus.ACTIVATED);

    mongoTemplate.insert(user);
  }

  @Test
  void testRegisterImageIntegration() throws Exception {
    ImageUploadRequest imageUploadRequest = new ImageUploadRequest(
        "https://res.cloudinary.com/v1/image/upload/newImage.jpg",
        new ObjectId().toString(),
        new ObjectId().toString());
    HttpEntity<ImageUploadRequest> request = generateHeader(
        imageUploadRequest);

    // ACT: Send POST request to register a new image
    ResponseEntity<ImageResponse> response = restTemplate.postForEntity("/api/v1/images", request,
        ImageResponse.class);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    List<Image> images = mongoTemplate.findAll(Image.class);
    assertEquals(6, images.size());
  }

  private <T> HttpEntity<T> generateHeader(T imageUploadRequest) {
    String token = loginUtils.obtenerTokenUsuario();
    HttpHeaders headers = loginUtils.crearHeadersConToken(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(imageUploadRequest, headers);
  }

  @Test
  void testGetImageByIdIntegration() throws Exception {
    // ARRANGE: Persist a test image in the database
    String token = loginUtils.obtenerTokenUsuario();
    HttpHeaders headers = loginUtils.crearHeadersConToken(token);

    HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

    // ACT: Send GET request to register a new image
    ResponseEntity<ImageResponse> response = restTemplate.exchange(
        "/api/v1/images/" + testImages.get(0).getId().toHexString(),
        HttpMethod.GET, httpEntity,
        ImageResponse.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(testImages.get(0).getId().toString(), response.getBody().id().toString());
    assertEquals(testImages.get(0).getImageUrl(), response.getBody().imageUrl());
  }

  @Test
  void testDeactivateImageIntegration() throws Exception {
    // ARRANGE: Persist a test image in the database
    String token = loginUtils.obtenerTokenUsuario();
    HttpHeaders headers = loginUtils.crearHeadersConToken(token);

    HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

    ResponseEntity<Void> response = restTemplate.exchange("/api/v1/images/" + testImages.get(0).getId().toHexString(),
        HttpMethod.DELETE, httpEntity,
        Void.class);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    List<Image> images = mongoTemplate.findAll(Image.class);
    assertEquals(4, images.size());
  }
}