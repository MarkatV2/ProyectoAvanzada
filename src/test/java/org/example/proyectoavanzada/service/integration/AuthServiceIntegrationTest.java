package org.example.proyectoavanzada.service.integration;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.dto.user.JwtAccessResponse;
import co.edu.uniquindio.proyecto.dto.user.JwtResponse;
import co.edu.uniquindio.proyecto.dto.user.LoginRequest;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exception.auth.AccountDisabledException;
import co.edu.uniquindio.proyecto.exception.user.InvalidPasswordException;
import co.edu.uniquindio.proyecto.exception.user.InvalidRefreshTokenException;
import co.edu.uniquindio.proyecto.exception.user.RefreshTokenExpiredException;
import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.service.implementations.UserDetailsServiceImplements;
import co.edu.uniquindio.proyecto.service.interfaces.AuthService;
import co.edu.uniquindio.proyecto.util.JwtUtils;
import co.edu.uniquindio.proyecto.util.KeyUtils;
import io.jsonwebtoken.Jwts;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(classes = ProyectoApplication.class)
public class AuthServiceIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private KeyUtils keyUtils;
    @MockitoBean
    private EmailService emailService;
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        testUsers = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> {
                    User user = new User();
                    user.setEmail("user" + i + "@example.com");
                    user.setPassword(encoder.encode("password" + i));
                    user.setAccountStatus(i == 3 ? AccountStatus.REGISTERED : AccountStatus.ACTIVATED);
                    user.setRol(i == 5 ? Rol.ADMIN : Rol.USER);
                    return userRepository.save(user);
                })
                .toList();
        userRepository.saveAll(testUsers);
    }


    // ------------------------------------------- LOGIN -------------------------------------------- //


    @Test
    @DisplayName("authenticate - éxito con usuario activado")
    void authenticate_Success() {
        var user = testUsers.get(0); // ACTIVATED
        var request = new LoginRequest(user.getEmail(), "password1");

        JwtResponse response = authService.authenticate(request);

        assertNotNull(response.token());
        assertNotNull(response.refreshToken());
    }

    @Test
    @DisplayName("authenticate - error por contraseña incorrecta")
    void authenticate_InvalidPassword() {
        var user = testUsers.get(1); // ACTIVATED
        var request = new LoginRequest(user.getEmail(), "wrongpassword");

        assertThrows(InvalidPasswordException.class, () -> authService.authenticate(request));
    }

    @Test
    @DisplayName("authenticate - error por cuenta no activada")
    void authenticate_AccountNotActivated() {
        var user = testUsers.get(2); // REGISTERED
        var request = new LoginRequest(user.getEmail(), "password3");

        assertThrows(AccountDisabledException.class, () -> authService.authenticate(request));
    }

    @Test
    @DisplayName("authenticate - lanza UserNotFoundException si el usuario no existe")
    void authenticate_UserNotFound() {
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "somepassword");

        AuthenticationServiceException ex = assertThrows(AuthenticationServiceException.class,
                () -> authService.authenticate(loginRequest));

        assertTrue(ex.getCause() instanceof UserNotFoundException);
    }


    @Test
    @DisplayName("authenticate - éxito con usuario administrador")
    void authenticate_Success_Admin() {
        var user = testUsers.get(4); // ADMIN
        var request = new LoginRequest(user.getEmail(), "password5");

        JwtResponse response = authService.authenticate(request);

        assertNotNull(response.token());
        assertNotNull(response.refreshToken());
    }


    // ------------------------------------------- REFRESH_ACCESS_TOKEN -------------------------------------------- //


    @Test
    @DisplayName("refreshAccessToken - retorna nuevo token si el refresh es válido")
    void refreshAccessToken_Success() {
        User user = testUsers.get(0);
        String refreshToken = jwtUtils.generateRefreshToken(user);

        JwtAccessResponse response = authService.refreshAccessToken(refreshToken);

        assertNotNull(response);
        assertNotNull(response.accessToken());
    }

    @Test
    @DisplayName("refreshAccessToken - lanza excepción si el token está expirado")
    void refreshAccessToken_ExpiredToken() {
        User user = testUsers.get(1);

        // Token expirado (manualmente firmado con duración 0)
        String expiredToken = Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId().toHexString())
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(KeyUtils.getPrivateKey())
                .compact();

        assertThrows(RefreshTokenExpiredException.class,
                () -> authService.refreshAccessToken(expiredToken));
    }

    @Test
    @DisplayName("refreshAccessToken - lanza excepción si el token es inválido")
    void refreshAccessToken_InvalidToken() {
        String invalidToken = "estoNoEsUnJwt";

        assertThrows(InvalidRefreshTokenException.class,
                () -> authService.refreshAccessToken(invalidToken));
    }

    @Test
    @DisplayName("refreshAccessToken - lanza excepción si el usuario no existe")
    void refreshAccessToken_UserNotFound() {
        String fakeUserId = new ObjectId().toHexString();

        String tokenWithInvalidUser = Jwts.builder()
                .subject("no-user@example.com")
                .claim("userId", fakeUserId)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 100000))
                .signWith(KeyUtils.getPrivateKey())
                .compact();

        assertThrows(UserNotFoundException.class,
                () -> authService.refreshAccessToken(tokenWithInvalidUser));
    }


}
