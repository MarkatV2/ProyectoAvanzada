package org.example.proyectoavanzada.controller.integration;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.configuration.SecurityConfig;
import co.edu.uniquindio.proyecto.controller.AuthController;
import co.edu.uniquindio.proyecto.dto.response.SuccessResponse;
import co.edu.uniquindio.proyecto.dto.user.PasswordUpdate;
import co.edu.uniquindio.proyecto.dto.user.UserRegistration;
import co.edu.uniquindio.proyecto.dto.user.UserResponse;
import co.edu.uniquindio.proyecto.dto.user.UserUpdateRequest;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exceptionhandler.auth.SecurityErrorHandler;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.service.EmailService;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(classes = {ProyectoApplication.class, LoginUtils.class, SecurityConfig.class})
@Import({AuthController.class, SecurityErrorHandler.class})
public class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @MockitoBean
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private LoginUtils loginUtils;

    @BeforeEach
    void setUp() {
        mongoTemplate.getDb().getCollection("users").drop();

        // Crear usuario admin para pruebas
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRol(Rol.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVATED);
        userRepository.save(admin);

        // Crear usuario normal
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("user123"));
        user.setRol(Rol.USER);
        user.setAccountStatus(AccountStatus.ACTIVATED);
        userRepository.save(user);
    }


    // ------------------------------------------- GET_ALL_USERS -------------------------------------------- //


    @Test
    @DisplayName("GET /api/v1/users - Admin accede correctamente (200 OK)")
    void getUsers_AdminAccess_ReturnsOk() {
        // Obtener token real
        String token = loginUtils.obtenerTokenAdmin();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);

        // Ejecutar petición
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users?page=1&size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/v1/users - Usuario normal no autorizado (403 Forbidden)")
    void getUsers_UserAccess_ReturnsForbidden() {
        String token = loginUtils.obtenerTokenUsuario();
        HttpHeaders headers = loginUtils.crearHeadersConToken(token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/v1/users - No autenticado (401 Unauthorized)")
    void getUsers_Unauthenticated_ReturnsUnauthorized() {
        // No incluir headers de autenticación
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().contains("Token de autenticación inválido o ausente"));
    }


    // ------------------------------------------- CREATE_USER -------------------------------------------- //


    @Test
    @DisplayName("POST /api/v1/users - Email duplicado (409 Conflict)")
    void registerUser_DuplicateEmail_ReturnsConflict() throws Exception {
        // Arrange: Crear usuario existente
        User existingUser = new User();
        existingUser.setEmail("existente@example.com");
        existingUser.setPassword("password");
        userRepository.save(existingUser);

        UserRegistration registration = new UserRegistration(
                "existente@example.com", // Email duplicado
                "Password123",
                "Nuevo Usuario",
                LocalDate.of(1990, 1, 1),
                "Ciudad de Prueba",
                10.0,
                4.7110,
                -74.0721
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users",
                HttpMethod.POST,
                new HttpEntity<>(registration, headers),
                String.class
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().contains("El correo ya está registrado"));
    }

    @Test
    @DisplayName("POST /api/v1/users - Registro exitoso (201 Created)")
    void registerUser_ValidData_ReturnsCreated() throws Exception {
        // Arrange
        UserRegistration registration = new UserRegistration(
                "nuevo@example.com",
                "Password123",
                "Nuevo Usuario",
                LocalDate.of(1990, 1, 1),
                "Ciudad de Prueba",
                10.0,
                4.7110,
                -74.0721
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Act
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                "/api/v1/users",
                HttpMethod.POST,
                new HttpEntity<>(registration, headers),
                UserResponse.class
        );

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(registration.email(), response.getBody().email());
        assertEquals(AccountStatus.REGISTERED.name(), response.getBody().accountStatus());

        // Verificar en base de datos
        User savedUser = userRepository.findByEmail(registration.email()).orElseThrow();
        assertEquals(registration.fullName(), savedUser.getFullName());
    }


    @Test
    @DisplayName("POST /api/v1/users - Datos inválidos (400 Bad Request)")
    void registerUser_InvalidData_ReturnsBadRequest() throws Exception {
        // Arrange: Datos inválidos (contraseña sin mayúsculas)
        UserRegistration invalidRegistration = new UserRegistration(
                "invalido@example.com",
                "password123", // Contraseña inválida
                "Nombre",
                LocalDate.of(1990, 1, 1),
                "Ciudad",
                10.0,
                4.7110,
                -74.0721
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users",
                HttpMethod.POST,
                new HttpEntity<>(invalidRegistration, headers),
                String.class
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("La contraseña debe contener al menos un dígito, una mayúscula y una minúscula"));
    }

    @Test
    @DisplayName("POST /api/v1/users - Fecha de nacimiento futura (400 Bad Request)")
    void registerUser_FutureBirthDate_ReturnsBadRequest() throws Exception {
        // Arrange: Fecha de nacimiento en el futuro
        UserRegistration invalidRegistration = new UserRegistration(
                "futuro@example.com",
                "Password123",
                "Nombre",
                LocalDate.now().plusDays(1), // Fecha inválida
                "Ciudad",
                10.0,
                4.7110,
                -74.0721
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users",
                HttpMethod.POST,
                new HttpEntity<>(invalidRegistration, headers),
                String.class
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("La fecha de nacimiento debe ser en el pasado"));
    }


    // ------------------------------------------- GET_USER_BY_ID -------------------------------------------- //


    @Test
    @DisplayName("GET /api/v1/users/{userId} - Admin obtiene usuario (200 OK)")
    void getUser_AdminAccess_ReturnsUser() throws Exception {
        // Arrange
        User testUser = crearUsuarioTest("user@test.com", Rol.USER);
        String adminToken = loginUtils.obtenerTokenAdmin();

        // Act
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                "/api/v1/users/" + testUser.getId(),
                HttpMethod.GET,
                new HttpEntity<>(loginUtils.crearHeadersConToken(adminToken)),
                UserResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testUser.getEmail(), response.getBody().email());
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId} - Usuario obtiene sus propios datos (200 OK)")
    void getUser_SelfAccess_ReturnsUser() throws Exception {
        // Arrange
        User testUser = crearUsuarioTest("self@test.com", Rol.USER);
        String userToken = loginUtils.obtenerTokenUsuario("self@test.com", "password");

        // Act
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                "/api/v1/users/" + testUser.getId(),
                HttpMethod.GET,
                new HttpEntity<>(loginUtils.crearHeadersConToken(userToken)),
                UserResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testUser.getEmail(), response.getBody().email());
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId} - Usuario no autorizado (403 Forbidden)")
    void getUser_OtherUserAccess_ReturnsForbidden() throws Exception {
        // Arrange
        User testUser = crearUsuarioTest("other@test.com", Rol.USER);
        crearUsuarioTest("unauth@test.com", Rol.USER);
        String unauthorizedToken = loginUtils.obtenerTokenUsuario("unauth@test.com", "password");

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + testUser.getId(),
                HttpMethod.GET,
                new HttpEntity<>(loginUtils.crearHeadersConToken(unauthorizedToken)),
                String.class
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId} - Usuario no existe (404 Not Found)")
    void getUser_NonExistentUser_ReturnsNotFound() throws Exception {
        // Arrange
        String nonExistentId = "507f1f77bcf86cd799439011"; // ID válido pero inexistente
        String adminToken = loginUtils.obtenerTokenAdmin();

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + nonExistentId,
                HttpMethod.GET,
                new HttpEntity<>(loginUtils.crearHeadersConToken(adminToken)),
                String.class
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId} - No autenticado (401 Unauthorized)")
    void getUser_Unauthenticated_ReturnsUnauthorized() throws Exception {
        // Arrange
        User testUser = crearUsuarioTest("user@test.com", Rol.USER);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + testUser.getId(),
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    // ------------------------------------------- UPDATE_USER -------------------------------------------- //


    @Test
    @DisplayName("PUT /api/v1/users/{id} - Actualización exitosa por propio usuario (200 OK)")
    void updateUser_SelfUser_ReturnsUpdatedUser() {
        // Arrange
        User testUser = crearUsuarioTest("usuario@test.com", Rol.USER);
        String token = loginUtils.obtenerTokenUsuario(testUser.getEmail(), "password");

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "nuevoemail@test.com",
                "Nuevo Nombre",
                LocalDate.of(1990, 1, 1),
                "Nueva Ciudad",
                4.7110,
                -74.0721
        );

        // Act
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                "/api/v1/users/" + testUser.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, loginUtils.crearHeadersConToken(token)),
                UserResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("nuevoemail@test.com", response.getBody().email());
        assertNotNull(response.getHeaders().getLocation());
    }


    @Test
    @DisplayName("PUT /api/v1/users/{id} - Actualización por admin (403)")
    void updateUser_AdminAccess_ReturnsForbidden() {
        // Arrange
        User testUser = crearUsuarioTest("usuario@test.com", Rol.USER);
        String adminToken = loginUtils.obtenerTokenAdmin();

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                testUser.getEmail(),
                "Nombre Actualizado",
                LocalDate.of(1985, 5, 15),
                "Otra Ciudad",
                4.7110,
                -74.0721
        );

        // Act
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                "/api/v1/users/" + testUser.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, loginUtils.crearHeadersConToken(adminToken)),
                UserResponse.class
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Acceso no autorizado (403 Forbidden)")
    void updateUser_UnauthorizedUser_ReturnsForbidden() {
        // Arrange
        User targetUser = crearUsuarioTest("target@test.com", Rol.USER);
        User unauthorizedUser = crearUsuarioTest("other@test.com", Rol.USER);
        String unauthorizedToken = loginUtils.obtenerTokenUsuario(unauthorizedUser.getEmail(), "password");

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "nuevo@test.com",
                "Nombre nuevo xd",
                LocalDate.now().minusYears(20),
                "Ciudad",
                4.7110,
                -74.0721
        );

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + targetUser.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, loginUtils.crearHeadersConToken(unauthorizedToken)),
                String.class
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Email ya existente (409 Conflict)")
    void updateUser_ExistingEmail_ReturnsConflict() {
        // Arrange
        User existingUser = crearUsuarioTest("existente@test.com", Rol.USER);
        User testUser = crearUsuarioTest("usuario@test.com", Rol.USER);
        String token = loginUtils.obtenerTokenUsuario(testUser.getEmail(), "password");

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "existente@test.com", // Email existente
                "Nombre nuevo xd",
                LocalDate.now().minusYears(20),
                "Ciudad",
                4.7110,
                -74.0721
        );

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + testUser.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, loginUtils.crearHeadersConToken(token)),
                String.class
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().contains("El correo ya está registrado"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Validación fallida (400 Bad Request)")
    void updateUser_InvalidData_ReturnsBadRequest() {
        // Arrange
        User testUser = crearUsuarioTest("usuario@test.com", Rol.USER);
        String token = loginUtils.obtenerTokenUsuario(testUser.getEmail(), "password");

        UserUpdateRequest invalidRequest = new UserUpdateRequest(
                "emailinvalido",
                "Nom", // Nombre demasiado corto
                LocalDate.now().plusDays(1), // Fecha futura
                "",
                200, // Latitud inválida
                200  // Longitud inválida
        );

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + testUser.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(invalidRequest, loginUtils.crearHeadersConToken(token)),
                String.class
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("El email debe tener un formato correcto"));
        assertTrue(response.getBody().contains("La fecha de nacimiento debe ser en el pasado"));
    }


    @Test
    @DisplayName("PUT /api/v1/users/{id} - No autenticado (401 Unauthorized)")
    void updateUser_Unauthenticated_ReturnsUnauthorized() {
        // Arrange
        User testUser = crearUsuarioTest("usuario@test.com", Rol.USER);

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "nuevo@test.com",
                "Nombre",
                LocalDate.now().minusYears(20),
                "Ciudad",
                4.7110,
                -74.0721
        );

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + testUser.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, new HttpHeaders()),
                String.class
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    // ------------------------------------------- UPDATE_PASSWORD -------------------------------------------- //


    @Test
    @DisplayName("PATCH /api/v1/users/{id}/password - Actualización exitosa (200 OK)")
    void updateUserPassword_ValidRequest_ReturnsSuccess() {
        // Arrange
        User testUser = crearUsuarioTest("usuario@test.com", Rol.USER);
        String oldPassword = "OldPassword123";
        testUser.setPassword(passwordEncoder.encode(oldPassword));
        userRepository.save(testUser);

        PasswordUpdate request = new PasswordUpdate(oldPassword, "NewPassword123");
        String token = loginUtils.obtenerTokenUsuario(testUser.getEmail(), oldPassword);

        // Act
        ResponseEntity<SuccessResponse> response = restTemplate.exchange(
                "/api/v1/users/" + testUser.getId() + "/password",
                HttpMethod.PATCH,
                new HttpEntity<>(request, loginUtils.crearHeadersConToken(token)),
                SuccessResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Contraseña actualizada exitosamente", response.getBody().message());

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NewPassword123", updatedUser.getPassword()));
    }


    @Test
    @DisplayName("PATCH /api/v1/users/{id}/password - Contraseña actual incorrecta (400 Bad Request)")
    void updateUserPassword_InvalidCurrentPassword_ReturnsBadRequest() {
        // Arrange
        User testUser = crearUsuarioTest("usuario@test.com", Rol.USER);
        testUser.setPassword(passwordEncoder.encode("CorrectPassword123"));
        userRepository.save(testUser);

        PasswordUpdate request = new PasswordUpdate("WrongPassword123", "NewPassword123");
        String token = loginUtils.obtenerTokenUsuario(testUser.getEmail(), "CorrectPassword123");

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + testUser.getId() + "/password",
                HttpMethod.PATCH,
                new HttpEntity<>(request, loginUtils.crearHeadersConToken(token)),
                String.class
        );


        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("La contraseña actual es incorrecta"));
    }


    @Test
    @DisplayName("PATCH /api/v1/users/{id}/password - Acceso no autorizado (403 Forbidden)")
    void updateUserPassword_UnauthorizedUser_ReturnsForbidden() {
        // Arrange
        User targetUser = crearUsuarioTest("target@test.com", Rol.USER);
        User otherUser = crearUsuarioTest("other@test.com", Rol.USER);
        String otherUserToken = loginUtils.obtenerTokenUsuario(otherUser.getEmail(), "password");

        PasswordUpdate request = new PasswordUpdate("password", "NewPassword123");

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + targetUser.getId() + "/password",
                HttpMethod.PATCH,
                new HttpEntity<>(request, loginUtils.crearHeadersConToken(otherUserToken)),
                String.class
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/password - Validación fallida (400 Bad Request)")
    void updateUserPassword_InvalidData_ReturnsBadRequest() {
        // Arrange
        User testUser = crearUsuarioTest("usuario@test.com", Rol.USER);
        String token = loginUtils.obtenerTokenUsuario(testUser.getEmail(), "password");

        PasswordUpdate invalidRequest = new PasswordUpdate("", "short");

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + testUser.getId() + "/password",
                HttpMethod.PATCH,
                new HttpEntity<>(invalidRequest, loginUtils.crearHeadersConToken(token)),
                String.class
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("La contraseña actual es obligatoria"));
        assertTrue(response.getBody().contains("La contraseña debe contener entre 8 y 50 carácteres"));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/password - No autenticado (401 Unauthorized)")
    void updateUserPassword_Unauthenticated_ReturnsUnauthorized() {
        // Arrange
        User testUser = crearUsuarioTest("usuario@test.com", Rol.USER);
        PasswordUpdate request = new PasswordUpdate("password", "NewPassword123");

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/users/" + testUser.getId() + "/password",
                HttpMethod.PATCH,
                new HttpEntity<>(request, new HttpHeaders()),
                String.class
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    // ------------------------------------------- DELETED_REPORT -------------------------------------------- //


    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Usuario elimina su propia cuenta (204 No Content)")
    void deleteUser_SelfDeletion_Success() throws Exception {
        // Arrange
        User testUser = crearUsuarioTest("delete_self@test.com", Rol.USER);
        String userToken = loginUtils.obtenerTokenUsuario("delete_self@test.com", "password");

        // Act
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/users/" + testUser.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(loginUtils.crearHeadersConToken(userToken)),
                Void.class
        );

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertFalse(userRepository.findById(testUser.getId()).isPresent());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Eliminar usuario inexistente (404 Not Found)")
    void deleteUser_NonExistentUser_NotFound() throws Exception {
        // Arrange
        String nonExistentId = "507f1f77bcf86cd799439011"; // ID válido pero inexistente
        String adminToken = loginUtils.obtenerTokenUsuario("admin@example.com", "admin123");

        // Act
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/users/" + nonExistentId,
                HttpMethod.DELETE,
                new HttpEntity<>(loginUtils.crearHeadersConToken(adminToken)),
                Void.class
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }


    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Usuario intenta eliminar a otro usuario (403 Forbidden)")
    void deleteUser_UserTriesToDeleteOtherUser_Forbidden() throws Exception {
        // Arrange
        User testUser1 = crearUsuarioTest("user1@test.com", Rol.USER);
        User testUser2 = crearUsuarioTest("user2@test.com", Rol.USER);
        String userToken = loginUtils.obtenerTokenUsuario("user1@test.com", "password");

        // Act
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/users/" + testUser2.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(loginUtils.crearHeadersConToken(userToken)),
                Void.class
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(userRepository.findById(testUser2.getId()).isPresent());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Usuario no autenticado (401 Unauthorized)")
    void deleteUser_UnauthenticatedUser_Unauthorized() throws Exception {
        // Arrange
        User testUser = crearUsuarioTest("unauth@test.com", Rol.USER);

        // Act
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/users/" + testUser.getId(),
                HttpMethod.DELETE,
                null,
                Void.class
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(userRepository.findById(testUser.getId()).isPresent());
    }


    // Método auxiliar para crear usuarios
    private User crearUsuarioTest(String email, Rol rol) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password"));
        user.setRol(rol);
        user.setAccountStatus(AccountStatus.ACTIVATED);
        return userRepository.save(user);
    }

}
