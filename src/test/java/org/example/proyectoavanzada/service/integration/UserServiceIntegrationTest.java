package org.example.proyectoavanzada.service.integration;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.dto.response.SuccessResponse;
import co.edu.uniquindio.proyecto.dto.user.*;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exception.global.ServiceUnavailableException;
import co.edu.uniquindio.proyecto.exception.user.EmailAlreadyExistsException;
import co.edu.uniquindio.proyecto.exception.user.InvalidPasswordException;
import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.service.interfaces.UserService;
import co.edu.uniquindio.proyecto.service.interfaces.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@SpringBootTest
@ContextConfiguration(classes = ProyectoApplication.class)
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;
    @MockitoBean
    private EmailService emailService; // Mock para evitar dependencias de correo

    @MockitoBean
    private VerificationService verificationService;
    private UserRegistration validRegistration;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final int TOTAL_TEST_USERS = 15;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private List<User> testUsers;
    private UserUpdateRequest validUpdateRequest;


    @BeforeEach
    void setUp() {
        Mockito.doNothing().when(emailService).sendVerificationEmail(any(), any());
        Mockito.doNothing().when(verificationService).generateAndSendCode(any(), any());

        // Limpiar datos existentes
        mongoTemplate.getDb().getCollection("users").drop();

        testUsers = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            User user = new User();
            user.setEmail("user" + i + "@example.com");
            user.setPassword(passwordEncoder.encode("password"));
            user.setFullName("User " + i);
            user.setDateBirth(LocalDateTime.now().minusYears(20));
            user.setRol(Rol.USER);
            user.setAccountStatus(AccountStatus.ACTIVATED);
            user.setCityOfResidence("City " + i);
            user.setNotificationRadiusKm(10.0);
            // Configuración opcional de ubicación
            user.setLocation(new GeoJsonPoint(-74.0060, 40.7128)); // Nueva York
            testUsers.add(user);
        }

        userRepository.saveAll(testUsers);

        // Datos de prueba válidos
        validRegistration = new UserRegistration(
                "test@example.com",
                "Password123",
                "Test User",
                LocalDate.of(1990, 1, 1),
                "Test City",
                10.0,
                4.7110,
                -74.0721
        );

        validUpdateRequest = new UserUpdateRequest(
                "newemail@example.com",
                "Nuevo Nombre",
                LocalDate.of(1990, 1, 1),
                "Nueva Ciudad",
                4.7110,
                -74.0721
        );
    }


    // ------------------------------------------- GET_ALL_USERS -------------------------------------------- //

    @Test
    @DisplayName("GET /users - Paginación básica (página 1, tamaño 10)")
    void testGetUsers_FirstPage_ReturnsPaginatedResults() {
        // Arrange
        int page = 1;
        int size = DEFAULT_PAGE_SIZE;

        // Act
        PaginatedUserResponse response = userService.getUsers(page, size);

        // Assert
        assertAll("Verificar paginación básica",
                () -> assertEquals(TOTAL_TEST_USERS, response.totalItems(), "El total de usuarios no coincide"),
                () -> assertEquals(2, response.totalPages(), "El total de páginas no coincide"),
                () -> assertEquals(page, response.currentPage(), "La página actual no coincide"),
                () -> assertEquals(size, response.users().size(), "El tamaño de la página no coincide")
        );
    }

    @Test
    @DisplayName("GET /users - Última página parcialmente llena (página 2, tamaño 10)")
    void testGetUsers_LastPartialPage_ReturnsRemainingUsers() {
        // Arrange
        int page = 2;
        int size = DEFAULT_PAGE_SIZE;
        int expectedItemsOnLastPage = TOTAL_TEST_USERS % size;

        // Act
        PaginatedUserResponse response = userService.getUsers(page, size);

        // Assert
        assertAll("Verificar última página parcial",
                () -> assertEquals(expectedItemsOnLastPage, response.users().size(), "El tamaño de la última página no coincide"),
                () -> assertFalse(response.users().isEmpty(), "La última página no debería estar vacía")
        );
    }

    @Test
    @DisplayName("GET /users - Parámetros inválidos (página 0, tamaño 200) se ajustan automáticamente")
    void testGetUsers_InvalidParameters_AutoAdjustsToValidRange() {
        // Arrange
        int invalidPage = 0;
        int invalidSize = 200;
        int expectedPage = 1;
        int expectedSize = 100; // Máximo permitido

        // Act
        PaginatedUserResponse response = userService.getUsers(invalidPage, invalidSize);

        // Assert
        assertAll("Verificar ajuste de parámetros inválidos",
                () -> assertEquals(expectedPage, response.currentPage(), "La página no se ajustó correctamente"),
                () -> assertTrue(response.users().size() <= expectedSize, "El tamaño no se ajustó correctamente")
        );
    }

    @Test
    @DisplayName("GET /users - Página fuera de rango retorna lista vacía")
    void testGetUsers_PageOutOfRange_ReturnsEmptyList() {
        // Arrange
        int outOfRangePage = 3;

        // Act
        PaginatedUserResponse response = userService.getUsers(outOfRangePage, DEFAULT_PAGE_SIZE);

        // Assert
        assertTrue(response.users().isEmpty(), "La lista debería estar vacía para páginas fuera de rango");
    }


    // ------------------------------------------- CREATE_USER -------------------------------------------- //

    @Test
    @DisplayName("Registro exitoso - Debe crear usuario y retornar respuesta válida")
    void registerUser_ValidData_ReturnsUserResponse() {
        // Act
        UserResponse response = userService.registerUser(validRegistration);

        // Assert
        assertAll(
                () -> assertNotNull(response.id(), "El ID no debe ser nulo"),
                () -> assertEquals(validRegistration.email(), response.email(), "El email no coincide"),
                () -> assertEquals(validRegistration.fullName(), response.fullName(), "El nombre no coincide"),
                () -> assertEquals(AccountStatus.REGISTERED.name(), response.accountStatus(), "El estado debe ser REGISTERED"),
                () -> assertEquals(validRegistration.cityOfResidence(), response.cityOfResidence(), "La ciudad no coincide")
        );

        // Verificar que se llamó al servicio de verificación
        verify(verificationService, times(1))
                .generateAndSendCode(any(User.class), any());
    }

    @Test
    @DisplayName("Registro con email existente - Debe lanzar EmailAlreadyExistsException")
    void registerUser_DuplicateEmail_ThrowsException() {
        // Arrange
        userService.registerUser(validRegistration); // Primer registro exitoso

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class,
                () -> userService.registerUser(validRegistration),
                "Debería lanzar EmailAlreadyExistsException para email duplicado");

        // Verificar que no se intentó guardar el segundo usuario
        assertEquals(16, userRepository.count(), "Solo deberían existir 16 usuarios");
    }


    // ------------------------------------------- GET_USER_BY_ID -------------------------------------------- //

    @Test
    @DisplayName("Consultar usuario existente - Debe retornar información correcta")
    void getUser_ExistingUser_ReturnsUserResponse() {
        // Arrange: Tomamos el primer usuario de la lista de prueba
        User expectedUser = testUsers.get(0);

        // Act
        UserResponse response = userService.getUser(expectedUser.getId().toString());

        // Assert
        assertAll(
                () -> assertNotNull(response, "La respuesta no debe ser nula"),
                () -> assertEquals(expectedUser.getId().toHexString(), response.id(), "El ID no coincide"),
                () -> assertEquals(expectedUser.getEmail(), response.email(), "El email no coincide"),
                () -> assertEquals(expectedUser.getFullName(), response.fullName(), "El nombre no coincide"),
                () -> assertEquals(expectedUser.getAccountStatus().name(), response.accountStatus(), "El estado no coincide"),
                () -> assertEquals(expectedUser.getCityOfResidence(), response.cityOfResidence(), "La ciudad no coincide")
        );
    }

    @Test
    @DisplayName("Consultar usuario inexistente - Debe lanzar UserNotFoundException")
    void getUser_NonExistentUser_ThrowsException() {
        // Arrange: Generamos un ID que no existe
        String nonExistentId = UUID.randomUUID().toString();

        // Act & Assert
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUser(nonExistentId)
        );

        assertEquals("Usuario no encontrado: " + nonExistentId, exception.getMessage(), "El ID en la excepción no coincide");
    }

    @Test
    @DisplayName("Consultar con ID nulo - Debe lanzar IllegalArgumentException")
    void getUser_NullId_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUser(null),
                "Debería lanzar IllegalArgumentException para ID nulo"
        );
    }


    // ------------------------------------------- UPDATE_USER -------------------------------------------- //

    @Test
    @DisplayName("Actualización exitosa - Debe modificar los datos correctamente")
    void updateUser_ValidData_ReturnsUpdatedUser() {
        // Arrange: Tomamos el primer usuario de prueba
        User originalUser = testUsers.get(0);
        String userId = originalUser.getId().toHexString();

        // Act
        UserResponse response = userService.updateUser(userId, validUpdateRequest);

        // Assert
        assertAll(
                () -> assertEquals(userId, response.id(), "El ID no debería cambiar"),
                () -> assertEquals(validUpdateRequest.email(), response.email(), "El email no se actualizó"),
                () -> assertEquals(validUpdateRequest.fullName(), response.fullName(), "El nombre no se actualizó"),
                () -> assertEquals(validUpdateRequest.cityOfResidence(), response.cityOfResidence(), "La ciudad no se actualizó")
        );

        // Verificar en la base de datos
        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertEquals(validUpdateRequest.email(), updatedUser.getEmail(), "El email no se guardó correctamente en BD");
    }

    @Test
    @DisplayName("Actualizar usuario inexistente - Debe lanzar UserNotFoundException")
    void updateUser_NonExistentUser_ThrowsException() {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();

        // Act & Assert
        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUser(nonExistentId, validUpdateRequest),
                "Debería lanzar UserNotFoundException"
        );
    }

    @Test
    @DisplayName("Actualizar con email existente - Debe lanzar EmailAlreadyExistsException")
    void updateUser_DuplicateEmail_ThrowsException() {
        // Arrange: Intentamos actualizar con el email de otro usuario existente
        User userToUpdate = testUsers.get(0);
        User otherUser = testUsers.get(1);

        UserUpdateRequest invalidRequest = new UserUpdateRequest(
                otherUser.getEmail(), // Email ya existente
                "Nuevo Nombre",
                LocalDate.now().minusYears(30),
                "Nueva Ciudad",
                4.7110,
                -74.0721
        );

        // Act & Assert
        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.updateUser(userToUpdate.getId().toHexString(), invalidRequest),
                "Debería lanzar EmailAlreadyExistsException"
        );
    }


    // ------------------------------------------- UPDATE_PASSWORD -------------------------------------------- //

    @Test
    @DisplayName("Actualización de contraseña exitosa - Debe retornar éxito")
    void updateUserPassword_ValidData_ReturnsSuccess() {
        // Arrange: Tomamos el primer usuario y preparamos datos
        User user = testUsers.get(0);
        PasswordUpdate update = new PasswordUpdate(
                "password", // Contraseña actual correcta
                "NewPassword123" // Nueva contraseña
        );

        // Act
        SuccessResponse response = userService.updateUserPassword(user.getId().toHexString(), update);

        // Assert
        assertAll(
                () -> assertEquals("Contraseña actualizada exitosamente", response.message()),
                () -> assertTrue(passwordEncoder.matches(
                        "NewPassword123",
                        userRepository.findById(user.getId()).get().getPassword()
                ), "La nueva contraseña no se guardó correctamente")
        );
    }

    @Test
    @DisplayName("Actualizar contraseña con usuario inexistente - Debe lanzar UserNotFoundException")
    void updateUserPassword_NonExistentUser_ThrowsException() {
        // Arrange
        PasswordUpdate update = new PasswordUpdate("Password123", "NewPassword123");

        // Act & Assert
        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUserPassword(UUID.randomUUID().toString(), update)
        );
    }

    @Test
    @DisplayName("Actualizar contraseña con contraseña actual incorrecta - Debe lanzar InvalidPasswordException")
    void updateUserPassword_WrongCurrentPassword_ThrowsException() {
        // Arrange
        User user = testUsers.get(0);
        PasswordUpdate update = new PasswordUpdate(
                "WrongPassword", // Contraseña actual incorrecta
                "NewPassword123"
        );

        // Act & Assert
        assertThrows(
                InvalidPasswordException.class,
                () -> userService.updateUserPassword(user.getId().toHexString(), update)
        );
    }


    // ------------------------------------------- DELETE_USER -------------------------------------------- //

    @Test
    @DisplayName("Eliminación lógica exitosa - Debe cambiar estado a DELETED")
    void deleteUser_ValidId_ReturnsSuccessAndUpdatesStatus() {
        // Arrange: Tomamos el primer usuario
        User userToDelete = testUsers.get(0);
        String userId = userToDelete.getId().toHexString();

        // Act
        SuccessResponse response = userService.deleteUser(userId);

        // Assert
        assertAll(
                () -> assertEquals("Usuario eliminado exitosamente", response.message()),
                () -> assertEquals(AccountStatus.DELETED,
                        userRepository.findById(userId).get().getAccountStatus(),
                        "El estado no se actualizó a DELETED")
        );
    }

    @Test
    @DisplayName("Eliminar usuario inexistente - Debe lanzar UserNotFoundException")
    void deleteUser_NonExistentUser_ThrowsException() {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();

        // Act & Assert
        assertThrows(
                UserNotFoundException.class,
                () -> userService.deleteUser(nonExistentId)
        );
    }


    @Test
    @DisplayName("Eliminar usuario - Verificar que solo cambia el estado (eliminación lógica)")
    void deleteUser_VerifySoftDelete() {
        // Arrange
        User originalUser = testUsers.get(0);
        String userId = originalUser.getId().toHexString();

        // Act
        userService.deleteUser(userId);
        User deletedUser = userRepository.findById(userId).orElseThrow();

        // Assert: Verificar que solo cambió el estado
        assertAll(
                () -> assertEquals(AccountStatus.DELETED, deletedUser.getAccountStatus()),
                () -> assertEquals(originalUser.getEmail(), deletedUser.getEmail()),
                () -> assertEquals(originalUser.getFullName(), deletedUser.getFullName()),
                () -> assertEquals(originalUser.getLocation(), deletedUser.getLocation())
        );
    }

}