package org.example.proyectoavanzada.repository;

import static org.junit.jupiter.api.Assertions.*;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@DataMongoTest
@ContextConfiguration(classes = ProyectoApplication.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        // Limpiar la base de datos antes de cada prueba
        mongoTemplate.getDb().drop();

        LocalDateTime now = LocalDateTime.now();

        // Crear usuarios de prueba directamente con new User()
        User activatedUser1 = new User();
        activatedUser1.setId(new ObjectId());
        activatedUser1.setEmail("activated1@example.com");
        activatedUser1.setPassword("encodedPassword1");
        activatedUser1.setFullName("Usuario Activado 1");
        activatedUser1.setDateBirth(now.minusYears(25));
        activatedUser1.setCreatedAt(now.minusDays(10));
        activatedUser1.setRol(Rol.USER);
        activatedUser1.setAccountStatus(AccountStatus.ACTIVATED);
        activatedUser1.setCityOfResidence("Armenia");
        activatedUser1.setNotificationRadiusKm(20.0);
        activatedUser1.setLocation(new GeoJsonPoint(-75.6816, 4.5377));

        User activatedUser2 = new User();
        activatedUser2.setId(new ObjectId());
        activatedUser2.setEmail("activated2@example.com");
        activatedUser2.setPassword("encodedPassword2");
        activatedUser2.setFullName("Usuario Activado 2");
        activatedUser2.setDateBirth(now.minusYears(30));
        activatedUser2.setCreatedAt(now.minusDays(5));
        activatedUser2.setRol(Rol.ADMIN);
        activatedUser2.setAccountStatus(AccountStatus.ACTIVATED);
        activatedUser2.setCityOfResidence("Pereira");
        activatedUser2.setNotificationRadiusKm(15.0);
        activatedUser2.setLocation(new GeoJsonPoint(-75.6907, 4.8087));

        User registeredUser = new User();
        registeredUser.setId(new ObjectId());
        registeredUser.setEmail("registered@example.com");
        registeredUser.setPassword("encodedPassword3");
        registeredUser.setFullName("Usuario Registrado");
        registeredUser.setDateBirth(now.minusYears(22));
        registeredUser.setCreatedAt(now.minusHours(2));
        registeredUser.setRol(Rol.USER);
        registeredUser.setAccountStatus(AccountStatus.REGISTERED);
        registeredUser.setCityOfResidence("Manizales");
        registeredUser.setNotificationRadiusKm(10.0);
        registeredUser.setLocation(new GeoJsonPoint(-75.5172, 5.0689));

        User deletedUser = new User();
        deletedUser.setId(new ObjectId());
        deletedUser.setEmail("deleted@example.com");
        deletedUser.setPassword("encodedPassword4");
        deletedUser.setFullName("Usuario Eliminado");
        deletedUser.setDateBirth(now.minusYears(40));
        deletedUser.setCreatedAt(now.minusDays(20));
        deletedUser.setRol(Rol.USER);
        deletedUser.setAccountStatus(AccountStatus.DELETED);
        deletedUser.setCityOfResidence("Bogotá");
        deletedUser.setNotificationRadiusKm(5.0);
        deletedUser.setLocation(new GeoJsonPoint(-74.0721, 4.7110));

        // Guardar usuarios en la base de datos
        testUsers = List.of(
                userRepository.save(activatedUser1),
                userRepository.save(activatedUser2),
                userRepository.save(registeredUser),
                userRepository.save(deletedUser)
        );
    }

    @Test
    @DisplayName("findAll - Debe retornar solo usuarios no eliminados con todos sus campos")
    void findAll_ShouldReturnOnlyNonDeletedUsersWithAllFields() {
        // Arrange
        PageRequest pageRequest = PageRequest.of(0, 10);

        // Act
        Page<User> resultPage = userRepository.findAll(pageRequest);
        List<User> users = resultPage.getContent();

        // Assert
        assertEquals(3, resultPage.getTotalElements()); // 3 usuarios no eliminados

        // Verificar campos de un usuario
        User firstUser = users.get(0);
        assertNotNull(firstUser.getId());
        assertNotNull(firstUser.getEmail());
        assertNotNull(firstUser.getFullName());
        assertNotNull(firstUser.getDateBirth());
        assertNotNull(firstUser.getCreatedAt());
        assertNotNull(firstUser.getRol());
        assertNotNull(firstUser.getAccountStatus());
        assertNotNull(firstUser.getCityOfResidence());
        assertTrue(firstUser.getNotificationRadiusKm() > 0);
        assertNotNull(firstUser.getLocation());

        // Verificar que no se incluyen usuarios eliminados
        assertTrue(users.stream().noneMatch(u -> u.getAccountStatus() == AccountStatus.DELETED));
    }


    @Test
    @DisplayName("findById - Usuario activado debe ser encontrado por ID")
    void findById_ShouldReturnUser_WhenNotDeleted() {
        // Arrange
        User expectedUser = testUsers.get(0); // activatedUser1

        // Act
        Optional<User> result = userRepository.findById(expectedUser.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedUser.getId(), result.get().getId());
        assertNotEquals(AccountStatus.DELETED, result.get().getAccountStatus());
    }

    @Test
    @DisplayName("findById - Usuario eliminado no debe ser retornado")
    void findById_ShouldReturnEmpty_WhenUserIsDeleted() {
        // Arrange
        User deletedUser = testUsers.get(3); // usuario con DELETED

        // Act
        Optional<User> result = userRepository.findById(deletedUser.getId());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByEmail - Usuario registrado debe ser encontrado por correo")
    void findByEmail_ShouldReturnUser_WhenNotDeleted() {
        // Arrange
        User expectedUser = testUsers.get(2); // registeredUser

        // Act
        Optional<User> result = userRepository.findByEmail(expectedUser.getEmail());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedUser.getEmail(), result.get().getEmail());
        assertNotEquals(AccountStatus.DELETED, result.get().getAccountStatus());
    }

    @Test
    @DisplayName("findByEmail - Usuario eliminado no debe ser retornado")
    void findByEmail_ShouldReturnEmpty_WhenUserIsDeleted() {
        // Arrange
        User deletedUser = testUsers.get(3);

        // Act
        Optional<User> result = userRepository.findByEmail(deletedUser.getEmail());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByEmail - Correo no registrado debe retornar vacío")
    void findByEmail_ShouldReturnEmpty_WhenEmailDoesNotExist() {
        // Arrange
        String nonExistentEmail = "noexiste@example.com";

        // Act
        Optional<User> result = userRepository.findByEmail(nonExistentEmail);

        // Assert
        assertTrue(result.isEmpty());
    }


    @Test
    @DisplayName("findById - No debe retornar usuarios con estado DELETED aunque existan")
    void findById_ShouldNotReturnDeletedUsers() {
        // Arrange
        User deletedUser = testUsers.get(3);

        // Act
        Optional<User> result = userRepository.findById(deletedUser.getId());

        // Assert
        assertFalse(result.isPresent());
    }
}