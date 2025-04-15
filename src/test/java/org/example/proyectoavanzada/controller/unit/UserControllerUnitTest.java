package org.example.proyectoavanzada.controller.unit;


import co.edu.uniquindio.proyecto.controller.UserController;
import co.edu.uniquindio.proyecto.dto.response.SuccessResponse;
import co.edu.uniquindio.proyecto.dto.user.*;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exception.global.ServiceUnavailableException;
import co.edu.uniquindio.proyecto.exception.user.EmailAlreadyExistsException;
import co.edu.uniquindio.proyecto.exception.user.InvalidPasswordException;
import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import co.edu.uniquindio.proyecto.exceptionhandler.global.GlobalExceptionHandler;
import co.edu.uniquindio.proyecto.exceptionhandler.user.UserExceptionHandler;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.service.interfaces.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.example.proyectoavanzada.configuration.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ContextConfiguration(classes = {UserController.class, TestSecurityConfig.class})
@Import({UserExceptionHandler.class, ErrorResponseBuilder.class, GlobalExceptionHandler.class})
class UserControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private UserRegistration validRequest;
    private List<UserResponse> testUsers;
    private User user;


    @BeforeEach
    void setUp() {
        testUsers = List.of(
                new UserResponse("661b0ed3793b8b4312fdd845", "user1@example.com", "User One", LocalDate.of(1990, 1, 1),
                        "ACTIVE", "Armenia", 4.531, -75.674),
                new UserResponse("661b0ed3793b8b4312fdd846", "user2@example.com", "User Two", LocalDate.of(1991, 2, 2),
                        "ACTIVE", "Pereira", 4.813, -75.694),
                new UserResponse("661b0ed3793b8b4312fdd847", "user3@example.com", "User Three", LocalDate.of(1992, 3, 3),
                        "INACTIVE", "Manizales", 5.070, -75.517),
                new UserResponse("661b0ed3793b8b4312fdd848", "user4@example.com", "User Four", LocalDate.of(1993, 4, 4),
                        "ACTIVE", "Medellín", 6.244, -75.573),
                new UserResponse("661b0ed3793b8b4312fdd849", "user5@example.com", "User Five", LocalDate.of(1994, 5, 5),
                        "PENDING", "Bogotá", 4.711, -74.072)
        );

        validRequest = new UserRegistration(
                testUsers.get(0).email(),
                "SecurePassword123",
                testUsers.get(0).fullName(),
                testUsers.get(0).dateBirth(),
                testUsers.get(0).cityOfResidence(),
                20.0,
                testUsers.get(0).latitude(),
                testUsers.get(0).longitude()
        );

        user = new User();
        user.setId(new ObjectId("661b0ed3793b8b4312fdd845"));
        user.setEmail("user@example.com");
        user.setFullName("Usuario Test");
        user.setPassword("$2a$10$hashedPassword"); // Contraseña encriptada
        user.setAccountStatus(AccountStatus.ACTIVATED);
        user.setRol(Rol.USER);
    }

    @Test
    @DisplayName("POST /users - Creación exitosa de usuario")
    void createUser_ValidRequest_ShouldReturnCreated() throws Exception {
        // Configurar mock del servicio usando el primer usuario de prueba
        when(userService.registerUser(any(UserRegistration.class)))
                .thenReturn(testUsers.get(0));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(testUsers.get(0).id()))
                .andExpect(jsonPath("$.email").value(testUsers.get(0).email()))
                .andExpect(jsonPath("$.fullName").value(testUsers.get(0).fullName()));

        verify(userService).registerUser(any(UserRegistration.class));
    }

    @Test
    @DisplayName("POST /users - Validación de campos requeridos")
    void createUser_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        UserRegistration invalidRequest = new UserRegistration(
                "", "", "", null, "", 0, 0, 0
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).registerUser(any());
    }

    @Test
    @DisplayName("POST /users - Email duplicado retorna conflicto (409)")
    void createUser_DuplicateEmail_ShouldReturnConflict() throws Exception {
        when(userService.registerUser(any(UserRegistration.class)))
                .thenThrow(new EmailAlreadyExistsException("El correo ya está registrado"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El correo ya está registrado"));

        verify(userService).registerUser(any(UserRegistration.class));
    }

    @Test
    @DisplayName("POST /users - Error en la base de datos retorna ServiceUnavailable (503)")
    void createUser_ServiceUnavailable_ShouldReturnServiceUnavailable() throws Exception {
        when(userService.registerUser(any(UserRegistration.class)))
                .thenThrow(new ServiceUnavailableException("Error al acceder a la base de datos"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Error al acceder a la base de datos"));

        verify(userService).registerUser(any(UserRegistration.class));
    }


    @Test
    @DisplayName("GET /users - Listado paginado exitoso")
    void getUsers_AsAdmin_ShouldReturnPaginatedResponse() throws Exception {
        // Arrange
        int page = 1; // Página solicitada (1-based)
        int size = 3;
        int totalItems = testUsers.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);

        // Mockear respuesta paginada
        PaginatedUserResponse mockResponse = new PaginatedUserResponse(
                totalItems,
                totalPages,
                page, // currentPage debe ser igual al parámetro page (1-based)
                testUsers.subList(0, Math.min(size, testUsers.size()))
        );

        when(userService.getUsers(page, size)).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(totalItems))
                .andExpect(jsonPath("$.totalPages").value(totalPages))
                .andExpect(jsonPath("$.currentPage").value(page)) // Debe ser 1
                .andExpect(jsonPath("$.users.length()").value(3));
    }

    @Test
    @DisplayName("GET /users - Segunda página de resultados")
    void getUsers_SecondPage_ShouldReturnRemainingUsers() throws Exception {
        // Arrange
        int page = 2;
        int size = 3;
        int totalItems = testUsers.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);

        // Calcular índices correctamente (página 2 con size=3 debería ser índices 3-5)
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalItems);
        List<UserResponse> expectedUsers = testUsers.subList(startIndex, endIndex);

        // Mockear respuesta paginada
        PaginatedUserResponse mockResponse = new PaginatedUserResponse(
                totalItems,
                totalPages,
                page,  // currentPage debe ser igual al parámetro page (2)
                expectedUsers
        );

        when(userService.getUsers(page, size)).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.length()").value(2))
                .andExpect(jsonPath("$.users[0].email").value(testUsers.get(3).email()))
                .andExpect(jsonPath("$.users[1].email").value(testUsers.get(4).email()))
                .andExpect(jsonPath("$.totalItems").value(totalItems))
                .andExpect(jsonPath("$.currentPage").value(page))
                .andExpect(jsonPath("$.totalPages").value(totalPages));
    }

    @Test
    @DisplayName("GET /users - Página vacía más allá del límite")
    void getUsers_PageBeyondLimit_ShouldReturnEmptyList() throws Exception {
        // Arrange
        int page = 3;
        int size = 3;
        int totalItems = testUsers.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);

        // Mockear respuesta vacía
        PaginatedUserResponse mockResponse = new PaginatedUserResponse(
                totalItems,
                totalPages,
                page,  // currentPage debe ser igual al parámetro page (3)
                Collections.emptyList()
        );

        when(userService.getUsers(page, size)).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.length()").value(0))
                .andExpect(jsonPath("$.totalItems").value(totalItems))
                .andExpect(jsonPath("$.currentPage").value(page))
                .andExpect(jsonPath("$.totalPages").value(totalPages));
    }


    @Test
    @DisplayName("GET /users/{userId} - ObtenerUsuario")
    void getUser_SelfRequest_ShouldReturnUser() throws Exception {
        // Arrange
        String userId = "user1-id";
        UserResponse mockResponse = new UserResponse(
                userId, "user1@test.com", "Usuario Uno",
                LocalDate.now(), "ACTIVE", "Bogotá", 4.711, -74.072
        );

        when(userService.getUser(userId)).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId));

        verify(userService).getUser(userId);
    }

    @Test
    @DisplayName("GET /users/{userId} - Usuario no encontrado")
    void getUser_UserNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        String invalidUserId = "usuario-inexistente";
        when(userService.getUser(invalidUserId)).thenThrow(new UserNotFoundException(invalidUserId));

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{userId}", invalidUserId))
                .andExpect(status().isNotFound()) // o el status que hayas definido en tu @ControllerAdvice
                .andExpect(jsonPath("$.message").value("Usuario no encontrado: " + invalidUserId));

        verify(userService).getUser(invalidUserId);
    }

    @Test
    @DisplayName("PUT /users/{id} - Actualización exitosa de usuario")
    void updateUser_ValidRequest_ShouldReturnOk() throws Exception {
        // Arrange
        String userId = "1";
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "nuevoemail@example.com",
                "Usuario Actualizado",
                LocalDate.of(1985, 6, 15),
                "Cali",
                3.451,   // latitud
                -76.532  // longitud
        );

        // Simula la respuesta del servicio de actualización (la estructura del response debe coincidir)
        UserResponse mockResponse = new UserResponse(
                userId,
                updateRequest.email(),
                updateRequest.fullName(),
                updateRequest.dateBirth(),
                "ACTIVE",   // suponiendo que el estado no cambie
                updateRequest.cityOfResidence(),
                updateRequest.latitude(),
                updateRequest.longitude()
        );

        when(userService.updateUser(eq(userId), any(UserUpdateRequest.class)))
                .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(updateRequest.email()))
                .andExpect(jsonPath("$.fullName").value(updateRequest.fullName()))
                .andExpect(jsonPath("$.dateBirth").value(updateRequest.dateBirth().toString()))
                .andExpect(jsonPath("$.cityOfResidence").value(updateRequest.cityOfResidence()));

        verify(userService).updateUser(eq(userId), any(UserUpdateRequest.class));
    }

    @Test
    @DisplayName("PUT /users/{id} - Usuario no encontrado retorna 404")
    void updateUser_UserNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        String userId = "usuario-inexistente";
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "noexiste@example.com",
                "Usuario Inexistente",
                LocalDate.of(1980, 1, 1),
                "Cartagena",
                10.0,
                -75.0
        );

        when(userService.updateUser(eq(userId), any(UserUpdateRequest.class)))
                .thenThrow(new UserNotFoundException(userId));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado: " + userId));

        verify(userService).updateUser(eq(userId), any(UserUpdateRequest.class));
    }

    @Test
    @DisplayName("PUT /users/{id} - Email duplicado retorna conflicto (409)")
    void updateUser_DuplicateEmail_ShouldReturnConflict() throws Exception {
        // Arrange
        String userId = "1";
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "duplicate@example.com", // Email que ya existe para otro usuario
                "Usuario Actualizado",
                LocalDate.of(1985, 6, 15),
                "Cali",
                3.451,
                -76.532
        );

        when(userService.updateUser(eq(userId), any(UserUpdateRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("El correo ya está registrado"));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El correo ya está registrado"));

        verify(userService).updateUser(eq(userId), any(UserUpdateRequest.class));
    }

    @Test
    @DisplayName("PUT /users/{id} - Request inválida retorna Bad Request (400)")
    void updateUser_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange: se crea una request con datos inválidos para provocar errores de validación
        String userId = "1";
        UserUpdateRequest invalidRequest = new UserUpdateRequest(
                "no-email",          // Email con formato incorrecto y menos de 8 caracteres
                "User",              // Nombre demasiado corto (menos de 8 caracteres)
                LocalDate.now().plusDays(1),  // Fecha de nacimiento en el futuro
                "",                  // Ciudad vacía
                0,                   // Latitud no válida (dependiendo de la lógica de validación, aunque aquí es un primitive)
                0                    // Longitud
        );

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // Se verifica que el servicio no sea llamado si la validación falla
        verify(userService, never()).updateUser(anyString(), any(UserUpdateRequest.class));
    }

    @Test
    @DisplayName("❌ PATCH /users/{id}/password - Usuario no existe")
    void updateUserPassword_ShouldReturn404_WhenUserNotFound() throws Exception {
        // Arrange
        String userId = "nonexistent123";
        PasswordUpdate passwordUpdate = new PasswordUpdate("Password1", "NewSecure123");

        when(userService.updateUserPassword(eq(userId), any(PasswordUpdate.class)))
                .thenThrow(new UserNotFoundException(userId));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/users/{id}/password", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordUpdate)))
                .andExpect(status().isNotFound());

        verify(userService).updateUserPassword(eq(userId), any(PasswordUpdate.class));
    }


    @Test
    @DisplayName("✅ PATCH /users/{id}/password - Contraseña actualizada correctamente")
    void updatePassword_Success() throws Exception {
        // Arrange
        String userId = testUsers.get(0).id();
        PasswordUpdate passwordUpdate = new PasswordUpdate("Password1", "NewPass123");
        SuccessResponse response = new SuccessResponse("Contraseña actualizada exitosamente");

        when(userService.updateUserPassword(eq(userId), any(PasswordUpdate.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/users/{id}/password", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(response.message()));

        verify(userService).updateUserPassword(eq(userId), any(PasswordUpdate.class));
    }


    @Test
    @DisplayName("❌ PATCH /users/{id}/password - Contraseña incorrecta debe retornar 400")
    void updatePassword_InvalidCurrentPassword() throws Exception {
        // Arrange
        String userId = testUsers.get(1).id();
        PasswordUpdate passwordUpdate = new PasswordUpdate("WrongPassword", "NewSecure123");

        when(userService.updateUserPassword(eq(userId), any(PasswordUpdate.class)))
                .thenThrow(new InvalidPasswordException("La contraseña actual es incorrecta"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/users/{id}/password", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordUpdate)))
                .andExpect(status().isBadRequest()) // ✅ lo importante es el código
                .andExpect(jsonPath("$.message").value("La contraseña actual es incorrecta"));
    }


    @Test
    @DisplayName("✅ DELETE /users/{id} - Eliminación exitosa")
    void deleteUser_ShouldReturnNoContent_WhenUserExists() throws Exception {
        // Arrange
        String userId = testUsers.get(0).id();

        doNothing().when(userService).deleteUser(userId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/{id}", userId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
    }

    @Test
    @DisplayName("❌ DELETE /users/{id} - Usuario no encontrado")
    void deleteUser_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        // Arrange
        String invalidId = "661b0ed3793b8b4312fdd999";

        doThrow(new UserNotFoundException(invalidId)).when(userService).deleteUser(invalidId); // ✅ SOLO si es void

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/{id}", invalidId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado: " + invalidId));

        verify(userService).deleteUser(invalidId);
    }



}