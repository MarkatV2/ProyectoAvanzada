package org.example.proyectoavanzada.controller.unit;

import co.edu.uniquindio.proyecto.controller.AuthController;
import co.edu.uniquindio.proyecto.dto.user.*;
import co.edu.uniquindio.proyecto.entity.auth.VerificationCode;
import co.edu.uniquindio.proyecto.entity.auth.VerificationCodeType;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exception.user.InvalidRefreshTokenException;
import co.edu.uniquindio.proyecto.exception.user.RefreshTokenExpiredException;
import co.edu.uniquindio.proyecto.exception.auth.AccountDisabledException;
import co.edu.uniquindio.proyecto.exception.auth.CodeExpiredException;
import co.edu.uniquindio.proyecto.exception.auth.InvalidCodeException;
import co.edu.uniquindio.proyecto.exception.user.InvalidPasswordException;
import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import co.edu.uniquindio.proyecto.exceptionhandler.auth.AuthExceptionHandler;
import co.edu.uniquindio.proyecto.exceptionhandler.global.GlobalExceptionHandler;
import co.edu.uniquindio.proyecto.exceptionhandler.user.UserExceptionHandler;
import co.edu.uniquindio.proyecto.service.interfaces.AuthService;
import co.edu.uniquindio.proyecto.service.interfaces.VerificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.example.proyectoavanzada.configuration.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = {AuthController.class, TestSecurityConfig.class})
@Import({AuthExceptionHandler.class, ErrorResponseBuilder.class, GlobalExceptionHandler.class,
        UserExceptionHandler.class})
class AuthControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VerificationService verificationService;

    @MockitoBean
    private AuthService authService;
    @Autowired
    private ObjectMapper objectMapper;

    private List<User> mockUsers;
    private List<VerificationCode> mockCodes;

    @BeforeEach
    void setUp() {
        // Cinco usuarios de prueba
        mockUsers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            User u = new User();
            u.setId(new ObjectId());
            u.setEmail("user" + i + "@example.com");
            mockUsers.add(u);
        }

        // Cinco códigos de activación de prueba
        mockCodes = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            VerificationCode vc = new VerificationCode();
            vc.setId(new ObjectId());
            vc.setCode("CODE" + i);
            vc.setUserId(mockUsers.get(i - 1).getId());
            vc.setCreatedAt(LocalDateTime.now().minusMinutes(i));
            vc.setExpiresAt(LocalDateTime.now().plusMinutes(15 - i));
            vc.setVerificationCodeType(VerificationCodeType.ACTIVATION);
            mockCodes.add(vc);
        }
    }

    // verifyAccount

    @Test
    @DisplayName("PATCH /api/v1/auth/activations retorna 200 cuando el código es válido")
    void testVerifyAccount_Success() throws Exception {
        String code = mockCodes.get(0).getCode();
        doNothing().when(verificationService).validateCodeActivation(code);

        mockMvc.perform(patch("/api/v1/auth/activations").param("code", code))
                .andExpect(status().isOk())
                .andExpect(content().string("Cuenta verificada exitosamente"));
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/activations retorna 400 cuando el código no existe")
    void testVerifyAccount_InvalidCode() throws Exception {
        String badCode = "BAD";
        doThrow(new InvalidCodeException("BAD")).when(verificationService).validateCodeActivation(badCode);

        mockMvc.perform(patch("/api/v1/auth/activations").param("code", badCode))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/activations retorna 500 cuando el código expiró")
    void testVerifyAccount_CodeExpired() throws Exception {
        String expired = mockCodes.get(1).getCode();
        doThrow(new CodeExpiredException("expired")).when(verificationService).validateCodeActivation(expired);

        mockMvc.perform(patch("/api/v1/auth/activations").param("code", expired))
                .andExpect(status().isBadRequest());
    }


    // sendCodeAgain

    @Test
    @DisplayName("POST /api/v1/auth/activations/{userId} retorna 204 cuando el usuario existe")
    void testSendCodeAgain_Success() throws Exception {
        String userId = mockUsers.get(0).getId().toHexString();
        doNothing().when(verificationService).resendCode(userId, VerificationCodeType.ACTIVATION);

        mockMvc.perform(post("/api/v1/auth/activations/{userId}", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/auth/activations/{userId} retorna 404 cuando el usuario no existe")
    void testSendCodeAgain_UserNotFound() throws Exception {
        String badId = "no-id";
        doThrow(new UserNotFoundException(badId))
                .when(verificationService).resendCode(badId, VerificationCodeType.ACTIVATION);

        mockMvc.perform(post("/api/v1/auth/activations/{userId}", badId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/auth/activations/{userId} retorna 400 cuando el ID es inválido")
    void testSendCodeAgain_InvalidId() throws Exception {
        String invalid = "xxx";
        doThrow(new IllegalArgumentException("invalid"))
                .when(verificationService).resendCode(invalid, VerificationCodeType.ACTIVATION);

        mockMvc.perform(post("/api/v1/auth/activations/{userId}", invalid))
                .andExpect(status().isBadRequest());
    }
    @Test
    @DisplayName("POST /api/v1/auth/sessions retorna 200 con el JWT y refreshToken cuando las credenciales son válidas")
    void testLogin_Success() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        JwtResponse jwt = new JwtResponse("jwt-token", "refresh-token");
        when(authService.authenticate(any(LoginRequest.class))).thenReturn(jwt);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(jwt.token()))
                .andExpect(jsonPath("$.refreshToken").value(jwt.refreshToken()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/sessions retorna 400 cuando el email o la contraseña están vacíos o mal formateados")
    void testLogin_InvalidFields() throws Exception {
        // Email vacío y sin formato válido
        LoginRequest invalidRequest = new LoginRequest("invalid-email", "");

        mockMvc.perform(post("/api/v1/auth/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", hasSize(2)));
    }


    @Test
    @DisplayName("POST /api/v1/auth/sessions retorna 403 cuando la cuenta está deshabilitada")
    void testLogin_AccountDisabled() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new AccountDisabledException("Cuenta deshabilitada"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/auth/sessions retorna 400 cuando la contraseña es incorrecta")
    void testLogin_InvalidPassword() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("user@example.com", "wrongpass");
        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new InvalidPasswordException("Contraseña incorrecta"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/sessions retorna 404 cuando el usuario no existe")
    void testLogin_UserNotFound() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("nonexistent@example.com", "pass");
        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new UserNotFoundException("nonexistent@example.com"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }


    @Test
    @DisplayName("POST /api/v1/auth/passwordCodes retorna 204 cuando se solicita el código correctamente")
    void testRequestPasswordReset_Success() throws Exception {
        String email = "user@example.com";
        doNothing().when(verificationService).sendPasswordResetCode(email);

        mockMvc.perform(post("/api/v1/auth/passwordCodes")
                        .param("email", email))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/auth/passwordCodes retorna 404 cuando el correo no está registrado")
    void testRequestPasswordReset_UserNotFound() throws Exception {
        String email = "notfound@example.com";
        doThrow(new UserNotFoundException(email))
                .when(verificationService).sendPasswordResetCode(email);

        mockMvc.perform(post("/api/v1/auth/passwordCodes")
                        .param("email", email))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/users/password retorna 200 cuando el código y contraseña son válidos")
    void testConfirmResetPassword_Success() throws Exception {
        // Arrange
        PasswordResetRequest request = new PasswordResetRequest("CODE123", "NewPassword123");
        doNothing().when(verificationService).resetPasswordWithCode(request.code(), request.newPassword());

        // Act & Assert
        mockMvc.perform(patch("/api/v1/auth/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contraseña actualizada exitosamente"));
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/users/password retorna 400 cuando el código es inválido")
    void testConfirmResetPassword_InvalidCode() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest("BAD_CODE", "1234");
        doThrow(new InvalidCodeException("Código inválido"))
                .when(verificationService).resetPasswordWithCode(request.code(), request.newPassword());

        mockMvc.perform(patch("/api/v1/auth/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/users/password retorna 400 cuando el código expiró")
    void testConfirmResetPassword_ExpiredCode() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest("EXPIRED_CODE", "1234");
        doThrow(new CodeExpiredException("expirado"))
                .when(verificationService).resetPasswordWithCode(request.code(), request.newPassword());

        mockMvc.perform(patch("/api/v1/auth/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/auth/users/password retorna 404 cuando no se encuentra el usuario")
    void testConfirmResetPassword_UserNotFound() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest("VALID", "1234");
        doThrow(new UserNotFoundException("ID"))
                .when(verificationService).resetPasswordWithCode(request.code(), request.newPassword());

        mockMvc.perform(patch("/api/v1/auth/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }


    // --- refresh token ---

    @Test
    @DisplayName("POST /api/v1/auth/accessTokens retorna 200 con nuevo token cuando el refresh token es válido")
    void testRefreshToken_Success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid.refresh.token");
        JwtAccessResponse response = new JwtAccessResponse("nuevo.jwt.token");
        when(authService.refreshAccessToken(request.refreshToken())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/accessTokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(response.accessToken()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/accessTokens retorna 400 cuando el refresh token es inválido")
    void testRefreshToken_InvalidToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid.token");
        doThrow(new InvalidRefreshTokenException("Token inválido"))
                .when(authService).refreshAccessToken(request.refreshToken());

        mockMvc.perform(post("/api/v1/auth/accessTokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/accessTokens retorna 400 cuando el refresh token ha expirado")
    void testRefreshToken_ExpiredToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("expired.token");
        doThrow(new RefreshTokenExpiredException("Token expirado"))
                .when(authService).refreshAccessToken(request.refreshToken());

        mockMvc.perform(post("/api/v1/auth/accessTokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/accessTokens retorna 404 cuando no se encuentra el usuario del refresh token")
    void testRefreshToken_UserNotFound() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("token.de.usuario.inexistente");
        doThrow(new UserNotFoundException("no existe"))
                .when(authService).refreshAccessToken(request.refreshToken());

        mockMvc.perform(post("/api/v1/auth/accessTokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

}
