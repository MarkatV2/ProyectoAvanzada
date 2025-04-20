package org.example.proyectoavanzada.service.unit;


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
import co.edu.uniquindio.proyecto.service.implementations.AuthServiceImpl;
import co.edu.uniquindio.proyecto.service.implementations.UserDetailsServiceImplements;
import co.edu.uniquindio.proyecto.util.JwtUtils;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private UserDetailsServiceImplements userDetailsService;

    @InjectMocks
    private AuthServiceImpl authService;

    // Datos de prueba
    private final String VALID_EMAIL = "test@example.com";
    private final String VALID_PASSWORD = "validPassword123";
    private final String VALID_REFRESH_TOKEN = "valid.refresh.token";
    private final String EXPIRED_REFRESH_TOKEN = "expired.refresh.token";
    private final String INVALID_REFRESH_TOKEN = "invalid.refresh.token";
    private final String USER_ID = "507f1f77bcf86cd799439011";

    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        // Configurar 5 usuarios de prueba
        testUsers = List.of(
                createTestUser("user1@example.com", "pass1", AccountStatus.ACTIVATED, Rol.USER),
                createTestUser("user2@example.com", "pass2", AccountStatus.ACTIVATED, Rol.ADMIN),
                createTestUser("user3@example.com", "pass3", AccountStatus.REGISTERED, Rol.USER),
                createTestUser("user4@example.com", "pass4", AccountStatus.DELETED, Rol.USER),
                createTestUser("user5@example.com", "pass5", AccountStatus.ACTIVATED, Rol.ADMIN)
        );
    }

    private User createTestUser(String email, String password, AccountStatus status, Rol rol) {
        User user = new User();
        user.setId(new ObjectId());
        user.setEmail(email);
        user.setPassword(password);
        user.setAccountStatus(status);
        user.setRol(rol);
        return user;
    }


    // -----------------------------------------------AUTHENTICATE---------------------------------------


    @Test
    @DisplayName("authenticate - Debe retornar token JWT cuando credenciales son válidas")
    void authenticate_ShouldReturnJwt_WhenValidCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest(VALID_EMAIL, VALID_PASSWORD);
        User testUser = testUsers.get(0); // Usuario activado

        when(userDetailsService.loadUserByUsername(VALID_EMAIL)).thenReturn(testUser);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities()));
        String JWT_TOKEN = "generated.jwt.token";
        when(jwtUtils.generateToken(testUser)).thenReturn(JWT_TOKEN);
        String REFRESH_TOKEN = "generated.refresh.token";
        when(jwtUtils.generateRefreshToken(testUser)).thenReturn(REFRESH_TOKEN);

        // Act
        JwtResponse response = authService.authenticate(request);

        // Assert
        assertNotNull(response);
        assertEquals(JWT_TOKEN, response.token());
        assertEquals(REFRESH_TOKEN, response.refreshToken());
        verify(userDetailsService).loadUserByUsername(VALID_EMAIL);
        verify(authenticationManager).authenticate(any());
        verify(jwtUtils).generateToken(testUser);
    }

    @Test
    @DisplayName("authenticate - Debe lanzar InvalidPasswordException cuando contraseña es incorrecta")
    void authenticate_ShouldThrowInvalidPassword_WhenWrongPassword() {
        // Arrange
        String INVALID_PASSWORD = "wrongPassword";
        LoginRequest request = new LoginRequest(VALID_EMAIL, INVALID_PASSWORD);

        when(userDetailsService.loadUserByUsername(VALID_EMAIL))
                .thenReturn(testUsers.get(0));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        // Act & Assert
        assertThrows(InvalidPasswordException.class, () -> {
            authService.authenticate(request);
        });
    }

    @Test
    @DisplayName("authenticate - Debe lanzar UserNotFoundException cuando usuario no existe")
    void authenticate_ShouldThrowUserNotFound_WhenUserDoesNotExist() {
        // Arrange
        String INVALID_EMAIL = "nonexistent@example.com";
        LoginRequest request = new LoginRequest(INVALID_EMAIL, VALID_PASSWORD);

        when(userDetailsService.loadUserByUsername(INVALID_EMAIL))
                .thenThrow(new UsernameNotFoundException("Usuario no encontrado"));

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            authService.authenticate(request);
        });
    }

    @Test
    @DisplayName("authenticate - Debe lanzar AccountDisabledException cuando cuenta no está activada")
    void authenticate_ShouldThrowAccountDisabled_WhenAccountNotActivated() {
        // Arrange
        LoginRequest request = new LoginRequest(testUsers.get(2).getEmail(), "pass1");
        User inactiveUser = testUsers.get(2);// Usuario con status PENDING

        when(userDetailsService.loadUserByUsername(testUsers.get(2).getEmail())).thenReturn(inactiveUser);
        when(authenticationManager.authenticate(any()))
                .thenThrow(DisabledException.class);

        // Act & Assert
        assertThrows(AccountDisabledException.class, () -> {
            authService.authenticate(request);
        });
    }

    @Test
    @DisplayName("authenticate - Debe lanzar AuthenticationServiceException para errores inesperados")
    void authenticate_ShouldThrowAuthServiceException_ForUnexpectedErrors() {
        // Arrange
        LoginRequest request = new LoginRequest(VALID_EMAIL, VALID_PASSWORD);

        when(userDetailsService.loadUserByUsername(VALID_EMAIL))
                .thenThrow(new RuntimeException("Error inesperado"));

        // Act & Assert
        assertThrows(AuthenticationServiceException.class, () -> {
            authService.authenticate(request);
        });
    }

    @Test
    @DisplayName("authenticate - Debe lanzar InvalidPasswordException cuando request es inválido")
    void authenticate_ShouldThrowInvalidPassword_WhenRequestIsInvalid() {
        // Arrange
        LoginRequest nullRequest = new LoginRequest(null, null);
        LoginRequest blankRequest = new LoginRequest("", "");

        // Act & Assert
        assertAll(
                () -> assertThrows(InvalidPasswordException.class, () -> authService.authenticate(nullRequest)),
                () -> assertThrows(InvalidPasswordException.class, () -> authService.authenticate(blankRequest))
        );
    }


    //-------------------------------------REFRESH_ACCESS_TOKEN-------------------------------------------


    @Test
    @DisplayName("refreshAccessToken - Debe generar nuevo access token con refresh token válido")
    void refreshAccessToken_ShouldReturnNewAccessToken_WithValidRefreshToken() {
        // Arrange
        User testUser = new User();
        testUser.setId(new ObjectId(USER_ID));

        // Configurar mocks para flujo exitoso
        doNothing().when(jwtUtils).validateRefreshToken(VALID_REFRESH_TOKEN);
        when(jwtUtils.extractUserId(VALID_REFRESH_TOKEN)).thenReturn(USER_ID);
        when(userRepository.findById(new ObjectId(USER_ID))).thenReturn(Optional.of(testUser));
        String NEW_ACCESS_TOKEN = "new.access.token";
        when(jwtUtils.generateToken(testUser)).thenReturn(NEW_ACCESS_TOKEN);

        // Act
        JwtAccessResponse response = authService.refreshAccessToken(VALID_REFRESH_TOKEN);

        // Assert
        assertNotNull(response);
        assertEquals(NEW_ACCESS_TOKEN, response.accessToken());

        verify(jwtUtils).validateRefreshToken(VALID_REFRESH_TOKEN);
        verify(jwtUtils).extractUserId(VALID_REFRESH_TOKEN);
        verify(userRepository).findById(new ObjectId(USER_ID));
        verify(jwtUtils).generateToken(testUser);
    }

    @Test
    @DisplayName("refreshAccessToken - Debe lanzar RefreshTokenExpiredException con token expirado")
    void refreshAccessToken_ShouldThrow_WhenRefreshTokenExpired() {
        // Arrange
        doThrow(new RefreshTokenExpiredException("El refresh token ha expirado"))
                .when(jwtUtils).validateRefreshToken(EXPIRED_REFRESH_TOKEN);

        // Act & Assert
        assertThrows(RefreshTokenExpiredException.class, () -> {
            authService.refreshAccessToken(EXPIRED_REFRESH_TOKEN);
        });

        verify(jwtUtils).validateRefreshToken(EXPIRED_REFRESH_TOKEN);
        verifyNoInteractions(userRepository);
        verify(jwtUtils, never()).generateToken(any());
    }

    @Test
    @DisplayName("refreshAccessToken - Debe lanzar InvalidRefreshTokenException con token inválido")
    void refreshAccessToken_ShouldThrow_WhenInvalidRefreshToken() {
        // Arrange
        doThrow(new InvalidRefreshTokenException("El refresh token es inválido"))
                .when(jwtUtils).validateRefreshToken(INVALID_REFRESH_TOKEN);

        // Act & Assert
        assertThrows(InvalidRefreshTokenException.class, () -> {
            authService.refreshAccessToken(INVALID_REFRESH_TOKEN);
        });

        verify(jwtUtils).validateRefreshToken(INVALID_REFRESH_TOKEN);
        verifyNoInteractions(userRepository);
        verify(jwtUtils, never()).generateToken(any());
    }

    @Test
    @DisplayName("refreshAccessToken - Debe lanzar UsernameNotFoundException cuando usuario no existe")
    void refreshAccessToken_ShouldThrow_WhenUserNotFound() {
        // Arrange
        doNothing().when(jwtUtils).validateRefreshToken(VALID_REFRESH_TOKEN);
        when(jwtUtils.extractUserId(VALID_REFRESH_TOKEN)).thenReturn(USER_ID);
        when(userRepository.findById(new ObjectId(USER_ID))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            authService.refreshAccessToken(VALID_REFRESH_TOKEN);
        });

        verify(userRepository).findById(new ObjectId(USER_ID));
        verify(jwtUtils, never()).generateToken(any());
    }

    @Test
    @DisplayName("refreshAccessToken - Debe lanzar IllegalArgumentException cuando userId es inválido")
    void refreshAccessToken_ShouldThrow_WhenInvalidUserIdFormat() {
        // Arrange
        String invalidUserId = "invalid-id-format";
        doNothing().when(jwtUtils).validateRefreshToken(VALID_REFRESH_TOKEN);
        when(jwtUtils.extractUserId(VALID_REFRESH_TOKEN)).thenReturn(invalidUserId);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            authService.refreshAccessToken(VALID_REFRESH_TOKEN);
        });

        verify(userRepository, never()).findById((ObjectId) any());
        verify(jwtUtils, never()).generateToken(any());
    }

}