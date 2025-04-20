package org.example.proyectoavanzada.controller.integration;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.configuration.SecurityConfig;
import co.edu.uniquindio.proyecto.dto.response.SuccessResponse;
import co.edu.uniquindio.proyecto.dto.user.*;
import co.edu.uniquindio.proyecto.entity.auth.VerificationCode;
import co.edu.uniquindio.proyecto.entity.auth.VerificationCodeType;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exceptionhandler.auth.SecurityErrorHandler;
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.util.JwtUtils;
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
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(classes = {ProyectoApplication.class, LoginUtils.class, SecurityConfig.class})
@Import({SecurityErrorHandler.class})
public class AuthControllerIntegrationTest {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private EmailService emailService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;

    private List<User> testUsers;
    private List<VerificationCode> testCodes;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(User.class);
        mongoTemplate.dropCollection(VerificationCode.class);

        testUsers = IntStream.range(0, 5)
                .mapToObj(i -> {
                    User user = new User();
                    user.setId(new ObjectId());
                    user.setEmail("user" + i + "@example.com");
                    user.setPassword(passwordEncoder.encode("securePassword123"));
                    user.setFullName("User " + i);
                    user.setAccountStatus(i%2 == 0 ? AccountStatus.REGISTERED : AccountStatus.ACTIVATED);
                    user.setCreatedAt(LocalDateTime.now());
                    user.setRol(Rol.USER);
                    user.setNotificationRadiusKm(10.0);
                    user.setLocation(new GeoJsonPoint(0, 0));
                    return user;
                }).toList();

        mongoTemplate.insertAll(testUsers);

        testCodes = testUsers.stream()
                .map(user -> {
                    VerificationCode code = new VerificationCode();
                    code.setId(new ObjectId());
                    code.setCode(UUID.randomUUID().toString());
                    code.setUserId(user.getId());
                    code.setCreatedAt(LocalDateTime.now());
                    code.setExpiresAt(LocalDateTime.now().plusMinutes(15));
                    code.setVerificationCodeType(VerificationCodeType.ACTIVATION);
                    return code;
                }).toList();

        //Para actualizar Contraseña
        String code = "VALID1234";
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setCode(code);
        verificationCode.setUserId(testUsers.get(1).getId());
        verificationCode.setVerificationCodeType(VerificationCodeType.PASSWORD_RESET);
        verificationCode.setCreatedAt(LocalDateTime.now());
        verificationCode.setExpiresAt((LocalDateTime.now().plusMinutes(15)));

        mongoTemplate.insert(verificationCode);
        mongoTemplate.insertAll(testCodes);
    }


    // ------------------------------------------- VALIDATE_ACCOUNT -------------------------------------------- //


    @Test
    @DisplayName("Verificar cuenta con código válido devuelve 200 y activa al usuario")
    void givenValidCode_whenVerifyAccount_thenReturns200AndActivatesUser() {
        VerificationCode code = testCodes.get(0);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/auth/activations?code=" + code.getCode(),
                HttpMethod.PATCH,
                null,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("verificada"));

        User user = mongoTemplate.findById(code.getUserId(), User.class);
        assertNotNull(user);
        assertEquals(AccountStatus.ACTIVATED, user.getAccountStatus());

        assertFalse(mongoTemplate.exists(
                Query.query(Criteria.where("code").is(code.getCode())),
                VerificationCode.class
        ));
    }

    @Test
    @DisplayName("Código inválido devuelve 400")
    void givenInvalidCode_whenVerifyAccount_thenReturns400() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/auth/activations?code=invalid-code-123",
                HttpMethod.PATCH,
                null,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Código inválido"));
    }

    @Test
    @DisplayName("Código expirado devuelve 400 y elimina el código")
    void givenExpiredCode_whenVerifyAccount_thenReturns400AndDeletesCode() {
        VerificationCode expired = testCodes.get(1);
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        mongoTemplate.save(expired);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/auth/activations?code=" + expired.getCode(),
                HttpMethod.PATCH,
                null,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("expirado"));

        boolean stillExists = mongoTemplate.exists(
                Query.query(Criteria.where("code").is(expired.getCode())),
                VerificationCode.class
        );
        assertFalse(stillExists);
    }


    // ------------------------------------------- RESEND_CODE -------------------------------------------- //


    @Test
    @DisplayName("Reenvío de código exitoso elimina anteriores y responde 204")
    void givenValidUserId_whenSendCodeAgain_thenDeletesOldCodesAndReturns204() {
        User user = testUsers.get(2);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/v1/auth/activations/" + user.getId().toHexString(),
                null,
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        List<VerificationCode> remaining = mongoTemplate.find(
                Query.query(Criteria.where("userId").is(user.getId())),
                VerificationCode.class
        );

        assertEquals(1, remaining.size());
        assertEquals(user.getId(), remaining.get(0).getUserId());
    }

    @Test
    @DisplayName("Reenvío de código con usuario inexistente devuelve 404")
    void givenNonExistentUserId_whenSendCodeAgain_thenReturns404() {
        String fakeId = new ObjectId().toHexString();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/activations/" + fakeId,
                null,
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().contains("Usuario no encontrado"));
    }


    // ------------------------------------------- LOGIN -------------------------------------------- //


    @Test
    @DisplayName("Login exitoso devuelve 200 y tokens válidos")
    void givenValidCredentials_whenLogin_thenReturnsToken() {
        String email = testUsers.get(1).getEmail();
        String password = "securePassword123";

        LoginRequest request = new LoginRequest(email, password);

        ResponseEntity<JwtResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/sessions",
                request,
                JwtResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().token());
        assertNotNull(response.getBody().refreshToken());
    }

    @Test
    @DisplayName("Contraseña incorrecta devuelve 400 con mensaje")
    void givenWrongPassword_whenLogin_thenReturnsBadRequest() {
        String email = testUsers.get(1).getEmail();

        LoginRequest request = new LoginRequest(email, "wrongPassword");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/sessions",
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Contraseña incorrecta"));
    }

    @Test
    @DisplayName("Usuario inexistente devuelve 404")
    void givenNonExistentUser_whenLogin_thenReturnsNotFound() {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "somePassword");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/sessions",
                request,
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().contains("Usuario no encontrado"));
    }


    @Test
    @DisplayName("Cuenta no activada devuelve 403")
    void givenUnactivatedUser_whenLogin_thenReturnsForbidden() {
        String email = testUsers.get(0).getEmail();
        String password = "securePassword123";

        LoginRequest request = new LoginRequest(email, password);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/sessions",
                request,
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().contains("no está activada"));
    }


    // ------------------------------------------- PASSWORD_RESET_CODE -------------------------------------------- //


    @Test
    @DisplayName("Solicitud de recuperación con email existente devuelve 204")
    void givenExistingEmail_whenRequestPasswordReset_thenReturnsNoContent() {
        String email = testUsers.get(1).getEmail();

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/v1/auth/passwordCodes?email=" + email,
                null,
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    @DisplayName("Solicitud de recuperación con email inexistente devuelve 404")
    void givenNonExistentEmail_whenRequestPasswordReset_thenReturnsNotFound() {
        String email = "notfound@example.com";

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/passwordCodes?email=" + email,
                null,
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().contains("Usuario no encontrado"));
    }


    // ------------------------------------------- CONFIRM_RESET_PASSWORD -------------------------------------------- //


    @Test
    @DisplayName("Confirmación con código válido actualiza la contraseña y devuelve 200")
    void givenValidCode_whenConfirmReset_thenUpdatesPassword() {
        String code = "VALID1234";
        String email = testUsers.get(1).getEmail();

        PasswordResetRequest request = new PasswordResetRequest(code, "NewPassword1");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PasswordResetRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<SuccessResponse> response = restTemplate.exchange(
                "/api/v1/auth/users/password",
                HttpMethod.PATCH,
                entity,
                SuccessResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Contraseña actualizada exitosamente", response.getBody().message());

    }

    @Test
    @DisplayName("Confirmación con código inválido devuelve 400")
    void givenInvalidCode_whenConfirmReset_thenReturnsBadRequest() {
        PasswordResetRequest request = new PasswordResetRequest("INVALID_CODE", "NewPassword1");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PasswordResetRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/auth/users/password",
                HttpMethod.PATCH,
                entity,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }


    @Test
    @DisplayName("Confirmación con contraseña inválida devuelve 400 con mensaje de validación")
    void givenInvalidPasswordPattern_whenConfirmReset_thenReturnsValidationError() {
        PasswordResetRequest request = new PasswordResetRequest("ANYCODE", "short");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PasswordResetRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/auth/users/password",
                HttpMethod.PATCH,
                entity,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("La contraseña debe contener"));
    }


    // ------------------------------------------- REFRESH_ACCESS_TOKEN -------------------------------------------- //


    @Test
    @DisplayName("Refresh con token válido devuelve nuevo access token")
    void givenValidRefreshToken_whenRefreshToken_thenReturnsNewAccessToken() {
        User user = testUsers.get(1);

        // Simular refresh token válido
        String refreshToken = jwtUtils.generateRefreshToken(user);
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RefreshTokenRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<JwtAccessResponse> response = restTemplate.exchange(
                "/api/v1/auth/accessTokens",
                HttpMethod.POST,
                entity,
                JwtAccessResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().accessToken());
    }

    @Test
    @DisplayName("Refresh con token inválido devuelve 400")
    void givenInvalidRefreshToken_whenRefreshToken_thenReturnsBadRequest() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid.token.value");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RefreshTokenRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/auth/accessTokens",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("refresh token es inválido"));
    }


}
