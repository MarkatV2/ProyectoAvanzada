package org.example.proyectoavanzada.service.unit;


import co.edu.uniquindio.proyecto.dto.response.SuccessResponse;
import co.edu.uniquindio.proyecto.dto.user.PasswordUpdate;
import co.edu.uniquindio.proyecto.dto.user.UserRegistration;
import co.edu.uniquindio.proyecto.dto.user.UserResponse;
import co.edu.uniquindio.proyecto.dto.user.UserUpdateRequest;
import co.edu.uniquindio.proyecto.entity.auth.VerificationCodeType;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exception.global.ServiceUnavailableException;
import co.edu.uniquindio.proyecto.exception.user.EmailAlreadyExistsException;
import co.edu.uniquindio.proyecto.exception.user.InvalidPasswordException;
import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.service.implementations.UserServiceImpl;
import co.edu.uniquindio.proyecto.service.interfaces.VerificationService;
import co.edu.uniquindio.proyecto.service.mapper.UserMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private VerificationService verificationService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserServiceImpl userService;
    private List<User> existingUsers;
    private UserRegistration newUserRequest;
    private User newUserEntity;
    private UserResponse expectedResponse;

    @BeforeEach
    void setUp() {
        existingUsers = new ArrayList<>();

        existingUsers.add(createUser("ana@example.com", "Ana López", 4.531, -75.674));
        existingUsers.add(createUser("bruno@example.com", "Bruno Díaz", 4.532, -75.675));
        existingUsers.add(createUser("camila@example.com", "Camila Gómez", 4.533, -75.676));
        existingUsers.add(createUser("daniel@example.com", "Daniel Ruiz", 4.534, -75.677));
        existingUsers.add(createUser("elena@example.com", "Elena Torres", 4.535, -75.678));

        newUserRequest = new UserRegistration(
                "nuevo@example.com",
                "Password1",
                "Nuevo Usuario",
                LocalDate.of(2000, 1, 1),
                "Armenia",
                20.0,
                4.536,
                -75.679
        );

        newUserEntity = createUser(newUserRequest.email(), newUserRequest.fullName(),
                newUserRequest.latitude(), newUserRequest.longitude());
        newUserEntity.setDateBirth(newUserRequest.dateBirth().atStartOfDay());

        expectedResponse = new UserResponse(
                newUserEntity.getId().toHexString(),
                newUserEntity.getEmail(),
                newUserEntity.getFullName(),
                newUserEntity.getDateBirth().toLocalDate(),
                newUserEntity.getAccountStatus().name(),
                newUserEntity.getCityOfResidence(),
                newUserEntity.getLocation().getY(),
                newUserEntity.getLocation().getX()
        );
    }

    private User createUser(String email, String fullName, double lat, double lon) {
        User user = new User();
        user.setId(new ObjectId());
        user.setEmail(email);
        user.setPassword("Password1");
        user.setFullName(fullName);
        user.setDateBirth(LocalDateTime.of(1990, 1, 1, 0, 0));
        user.setCreatedAt(LocalDateTime.now());
        user.setRol(Rol.USER);
        user.setAccountStatus(AccountStatus.REGISTERED);
        user.setCityOfResidence("Armenia");
        user.setNotificationRadiusKm(15.0);
        user.setLocation(new GeoJsonPoint(lon, lat));
        return user;
    }


    // ------------------------------------------- CREATE_USER -------------------------------------------- //


    @Test
    @DisplayName("✅ Registrar usuario exitosamente con base simulada de 5 usuarios")
    void testRegisterUserSuccess() {
        // Arrange
        when(userRepository.findByEmail(newUserRequest.email())).thenReturn(Optional.empty());
        when(userMapper.toUserEntity(newUserRequest)).thenReturn(newUserEntity);
        when(userRepository.save(any(User.class))).thenReturn(newUserEntity);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(expectedResponse);

        // Act
        UserResponse result = userService.registerUser(newUserRequest);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse.email(), result.email());
        assertEquals(expectedResponse.latitude(), result.latitude());
        assertEquals(expectedResponse.longitude(), result.longitude());
        assertEquals(expectedResponse.accountStatus(), result.accountStatus());

        verify(userRepository).findByEmail(newUserRequest.email());
        verify(userRepository).save(any(User.class));
        verify(verificationService).generateAndSendCode(any(), eq(VerificationCodeType.ACTIVATION));
    }

    @Test
    @DisplayName("❌ Error al registrar usuario con email ya existente")
    void testRegisterUserWithDuplicateEmail() {
        // Arrange
        String duplicateEmail = existingUsers.get(2).getEmail(); // Camila Gómez
        when(userRepository.findByEmail(duplicateEmail)).thenReturn(Optional.of(existingUsers.get(2)));

        UserRegistration duplicated = new UserRegistration(
                duplicateEmail,
                "Password1",
                "Alguien Más",
                LocalDate.of(1995, 5, 15),
                "Armenia",
                10.0,
                4.533,
                -75.676
        );

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class, () -> userService.registerUser(duplicated));

        verify(userRepository, never()).save(any());
        verify(verificationService, never()).generateAndSendCode(any(), any());
    }


    // ------------------------------------------- GET_USER_BY_ID -------------------------------------------- //


    @Test
    @DisplayName("✅ Obtener usuario por ID exitosamente")
    void testGetUserByIdSuccess() {
        // Arrange
        User existingUser = existingUsers.get(0); // Ej: Ana
        String userId = existingUser.getId().toHexString();

        UserResponse expected = new UserResponse(
                userId,
                existingUser.getEmail(),
                existingUser.getFullName(),
                existingUser.getDateBirth().toLocalDate(),
                existingUser.getAccountStatus().name(),
                existingUser.getCityOfResidence(),
                existingUser.getLocation().getY(),
                existingUser.getLocation().getX()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userMapper.toUserResponse(existingUser)).thenReturn(expected);

        // Act
        UserResponse result = userService.getUser(userId);

        // Assert
        assertNotNull(result);
        assertEquals(expected.id(), result.id());
        assertEquals(expected.email(), result.email());
        assertEquals(expected.fullName(), result.fullName());
        assertEquals(expected.latitude(), result.latitude());
        assertEquals(expected.longitude(), result.longitude());
        verify(userRepository).findById(userId);
        verify(userMapper).toUserResponse(existingUser);
    }

    @Test
    @DisplayName("❌ Error al obtener usuario por ID inexistente")
    void testGetUserByIdNotFound() {
        // Arrange
        String fakeId = new ObjectId().toHexString();
        when(userRepository.findById(fakeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.getUser(fakeId));
        verify(userRepository).findById(fakeId);
        verify(userMapper, never()).toUserResponse(any());
    }


    // ------------------------------------------- GET_ALL_USERS -------------------------------------------- //


    @Test
    @DisplayName("✅ Obtener usuarios paginados exitosamente con parámetros válidos")
    void testGetUsersSuccess() {
        // Arrange
        int page = 2;
        int size = 3;

        // Simular el contenido de la página 2
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, existingUsers.size());
        List<User> pageContent = existingUsers.subList(fromIndex, toIndex);

        Page<User> userPage = new PageImpl<>(
                pageContent,
                PageRequest.of(page - 1, size),
                existingUsers.size()
        );

        List<UserResponse> responseList = pageContent.stream().map(user ->
                new UserResponse(
                        user.getId().toHexString(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getDateBirth().toLocalDate(),
                        user.getAccountStatus().name(),
                        user.getCityOfResidence(),
                        user.getLocation().getY(),
                        user.getLocation().getX()
                )
        ).toList();

        when(userRepository.findAll(PageRequest.of(page - 1, size))).thenReturn(userPage);
        when(userMapper.toListResponse(pageContent)).thenReturn(responseList);

        // Act
        var result = userService.getUsers(page, size);

        // Assert
        assertEquals(existingUsers.size(), result.totalItems());
        assertEquals(page, result.currentPage());
        assertEquals(2, result.totalPages());
        assertEquals(toIndex - fromIndex, result.users().size());
    }

    @Test
    @DisplayName("⚠️ Ajuste de parámetros cuando page < 1 y size > 100")
    void testGetUsersWithInvalidParams() {
        // Arrange
        int page = -2;
        int size = 500;
        int adjustedSize = 100;

        Page<User> userPage = new PageImpl<>(existingUsers.subList(0, 5), PageRequest.of(0, adjustedSize), 5);
        when(userRepository.findAll(PageRequest.of(0, adjustedSize))).thenReturn(userPage);
        when(userMapper.toListResponse(any())).thenReturn(Collections.emptyList());

        // Act
        var result = userService.getUsers(page, size);

        // Assert
        assertEquals(5, result.totalItems());
        assertEquals(1, result.currentPage());
        assertEquals(1, result.totalPages());
    }

    @Test
    @DisplayName("❌ Error de MongoDB lanza ServiceUnavailableException")
    void testGetUsersThrowsMongoException() {
        // Arrange
        int page = 1;
        int size = 10;
        when(userRepository.findAll((Pageable) any())).thenThrow(new UncategorizedMongoDbException("Fake", null));

        // Act & Assert
        assertThrows(ServiceUnavailableException.class, () -> userService.getUsers(page, size));
    }

    @Test
    @DisplayName("❌ Error de acceso a datos lanza ServiceUnavailableException")
    void testGetUsersThrowsDataAccessException() {
        // Arrange
        int page = 1;
        int size = 10;
        when(userRepository.findAll((Pageable) any())).thenThrow(new DataAccessException("Fake DAO") {
        });

        // Act & Assert
        assertThrows(ServiceUnavailableException.class, () -> userService.getUsers(page, size));
    }


    // ------------------------------------------- UPDATE_USER -------------------------------------------- //


    @Test
    @DisplayName("✅ Actualizar usuario exitosamente")
    void testUpdateUserSuccess() {
        // Arrange
        User existingUser = existingUsers.get(0); // Ej: Ana
        String userId = existingUser.getId().toHexString();

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "nuevo_email@example.com",
                "Ana Actualizada",
                LocalDate.of(1991, 5, 20),
                "Cali",
                4.550,
                -75.670
        );

        // Email nuevo, no usado por otro usuario
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(updateRequest.email())).thenReturn(Optional.empty());

        // Simular el mapper que actualiza la entidad en memoria
        doAnswer(invocation -> {
            UserUpdateRequest req = invocation.getArgument(0);
            User user = invocation.getArgument(1);
            user.setEmail(req.email());
            user.setFullName(req.fullName());
            user.setDateBirth(req.dateBirth().atStartOfDay());
            user.setCityOfResidence(req.cityOfResidence());
            user.setLocation(new GeoJsonPoint(req.longitude(), req.latitude()));
            return null;
        }).when(userMapper).updateUserFromRequest(updateRequest, existingUser);

        when(userRepository.save(existingUser)).thenReturn(existingUser);

        UserResponse expected = new UserResponse(
                userId,
                updateRequest.email(),
                updateRequest.fullName(),
                updateRequest.dateBirth(),
                existingUser.getAccountStatus().name(),
                updateRequest.cityOfResidence(),
                updateRequest.latitude(),
                updateRequest.longitude()
        );
        when(userMapper.toUserResponse(existingUser)).thenReturn(expected);

        // Act
        UserResponse result = userService.updateUser(userId, updateRequest);

        // Assert
        assertEquals(expected.email(), result.email());
        assertEquals(expected.fullName(), result.fullName());
        assertEquals(expected.latitude(), result.latitude());
        assertEquals(expected.longitude(), result.longitude());

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail(updateRequest.email());
        verify(userRepository).save(existingUser);
        verify(userMapper).updateUserFromRequest(updateRequest, existingUser);
        verify(userMapper).toUserResponse(existingUser);
    }

    @Test
    @DisplayName("❌ Error al actualizar: usuario no encontrado")
    void testUpdateUserNotFound() {
        // Arrange
        String fakeId = new ObjectId().toHexString();
        when(userRepository.findById(fakeId)).thenReturn(Optional.empty());

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "nuevo@example.com",
                "Usuario Desconocido",
                LocalDate.of(1995, 1, 1),
                "Bogotá",
                4.60,
                -74.08
        );

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.updateUser(fakeId, updateRequest));
        verify(userRepository).findById(fakeId);
        verify(userRepository, never()).save(any());
    }


    @Test
    @DisplayName("❌ Error al actualizar: email ya registrado por otro usuario")
    void testUpdateUserEmailAlreadyExists() {
        // Arrange
        User targetUser = existingUsers.get(0); // Ana
        String userId = targetUser.getId().toHexString();
        User otherUser = existingUsers.get(1);  // Bruno

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                otherUser.getEmail(), // Usando el correo de otro usuario
                "Ana Hackeada",
                LocalDate.of(1990, 12, 12),
                "Medellín",
                4.56,
                -75.67
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
        when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class, () -> userService.updateUser(userId, updateRequest));

        verify(userRepository).findById(userId);
        verify(userRepository).findByEmail(otherUser.getEmail());
        verify(userRepository, never()).save(any());
    }


    // ------------------------------------------- UPDATE_PASSWORD -------------------------------------------- //


    @Test
    @DisplayName("✅ Cambiar contraseña exitosamente")
    void testUpdateUserPasswordSuccess() {
        // Arrange
        User existingUser = existingUsers.get(0);
        String userId = existingUser.getId().toHexString();

        String oldPassword = "Password1";
        String newPassword = "NuevaPass123";

        existingUser.setPassword("$2a$10$hashDeEjemplo"); // Simulación de contraseña actual encriptada

        PasswordUpdate passwordUpdate = new PasswordUpdate(oldPassword, newPassword);

        // Mocks
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(oldPassword, existingUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("$2a$10$hashNuevo");
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        // Act
        SuccessResponse response = userService.updateUserPassword(userId, passwordUpdate);

        // Assert
        assertNotNull(response);
        assertEquals("Contraseña actualizada exitosamente", response.message());
        assertEquals("$2a$10$hashNuevo", existingUser.getPassword());

        verify(userRepository).findById(userId);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("❌ Error al cambiar contraseña: usuario no encontrado")
    void testUpdateUserPasswordUserNotFound() {
        // Arrange
        String fakeId = new ObjectId().toHexString();
        PasswordUpdate update = new PasswordUpdate("pass123", "nueva456");

        when(userRepository.findById(fakeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.updateUserPassword(fakeId, update));

        verify(userRepository).findById(fakeId);
        verify(passwordEncoder, never()).matches(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Error al cambiar contraseña: contraseña actual incorrecta")
    void testUpdateUserPasswordIncorrectCurrent() {
        // Arrange
        User existingUser = existingUsers.get(1);
        String userId = existingUser.getId().toHexString();

        PasswordUpdate update = new PasswordUpdate("passwordIncorrecta", "nuevaSegura");

        existingUser.setPassword("$2a$10$hashCorrecto");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(update.currentPassword(), existingUser.getPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidPasswordException.class, () -> userService.updateUserPassword(userId, update));

        verify(userRepository).findById(userId);
        verify(passwordEncoder).matches(update.currentPassword(), existingUser.getPassword());
        verify(userRepository, never()).save(any());
    }


    // ------------------------------------------- DELETED_USER -------------------------------------------- //


    @Test
    @DisplayName("Eliminar usuario existente - Debe marcarlo como DELETED y retornar SuccessResponse")
    void deleteUser_WhenUserExists_ShouldMarkAsDeletedAndReturnSuccessResponse() {
        // Arrange
        String userId = existingUsers.get(0).getId().toHexString();
        User userToDelete = existingUsers.get(0);
        userToDelete.setAccountStatus(AccountStatus.ACTIVATED); // Asegurar que no está ya eliminado

        when(userRepository.findById(userId)).thenReturn(Optional.of(userToDelete));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SuccessResponse response = userService.deleteUser(userId);

        // Assert
        assertNotNull(response);
        assertEquals("Usuario eliminado exitosamente", response.message());

        // Verificar que el estado se cambió a DELETED
        assertEquals(AccountStatus.DELETED, userToDelete.getAccountStatus());

        // Verificar las interacciones con los mocks
        verify(userRepository).findById(userId);
        verify(userRepository).save(userToDelete);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Eliminar usuario no existente - Debe lanzar UserNotFoundException")
    void deleteUser_WhenUserNotExists_ShouldThrowUserNotFoundException() {
        // Arrange
        String nonExistentUserId = "507f1f77bcf86cd799439011"; // ID que no existe

        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.deleteUser(nonExistentUserId);
        });

        // Verificar que no se intentó guardar
        verify(userRepository).findById(nonExistentUserId);
        verify(userRepository, never()).save(any(User.class));
    }

}
