package co.edu.uniquindio.proyecto.Service;

import co.edu.uniquindio.proyecto.DTO.*;
import co.edu.uniquindio.proyecto.Entity.AccountStatus;
import co.edu.uniquindio.proyecto.Entity.Rol;
import co.edu.uniquindio.proyecto.Entity.User;
import co.edu.uniquindio.proyecto.Exception.EmailAlreadyExistsException;
import co.edu.uniquindio.proyecto.Exception.InvalidPasswordException;
import co.edu.uniquindio.proyecto.Exception.ServiceUnavailableException;
import co.edu.uniquindio.proyecto.Exception.UserNotFoundException;
import co.edu.uniquindio.proyecto.Repository.UserRepository;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.dao.DataAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PaginatedUserResponse getUsers(int page, int size) {
        try {
            // Validar y ajustar parámetros
            page = Math.max(page, 1);
            size = Math.min(Math.max(size, 1), 100);

            Pageable pageable = PageRequest.of(page - 1, size);
            Page<User> userPage = userRepository.findAll(pageable);

            return new PaginatedUserResponse(
                    (int) userPage.getTotalElements(),
                    userPage.getTotalPages(),
                    page,
                    userPage.getContent().stream()
                            .map(this::mapToUserResponse)
                            .collect(Collectors.toList())
            );

        } catch (UncategorizedMongoDbException e) {
            log.error("Error de MongoDB: {}", e.getMessage());
            throw new ServiceUnavailableException("Error al acceder a la base de datos");
        } catch (DataAccessException e) {
            log.error("Error de acceso a datos: {}", e.getMessage());
            throw new ServiceUnavailableException("Error de acceso a datos");
        }
    }

    public UserResponse registerUser(UserRegistration userRegistration) {
        // Verificar si el correo ya está registrado
        if (userRepository.findByEmail(userRegistration.email()).isPresent()) {
            throw new EmailAlreadyExistsException("El correo ya está registrado");
        }

        // Crear el usuario
        User user = new User();
        user.setEmail(userRegistration.email());
        user.setPassword(passwordEncoder.encode(userRegistration.password())); // Encriptar la contraseña
        user.setFullName(userRegistration.fullName());
        user.setDateBirth(userRegistration.dateBirth());
        user.setDateCreation(LocalDate.now());
        user.setRol(Rol.USER);
        user.setAccountStatus(AccountStatus.REGISTERED);
        user.setCityOfResidence(userRegistration.cityOfResidence());

        try {
            // Guardar el usuario en la base de datos
            User savedUser = userRepository.save(user);
            log.info("Usuario registrado exitosamente: {}", savedUser.getEmail());

            // Convertir a UserResponse
            return mapToUserResponse(user);
        } catch (DataAccessException e) {
            log.error("Error al registrar el usuario: {}", e.getMessage());
            throw new ServiceUnavailableException("Error al acceder a la base de datos");
        }
    }

    public UserResponse getUser(String userId) {
        // Buscar el usuario por ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        // Convertir a UserResponse
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(String id, UserUpdateRequest userUpdateRequest) {
        // Buscar el usuario por ID
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));

        // Verificar si el correo ya está registrado por otro usuario
        if (userRepository.findByEmail(userUpdateRequest.email()).isPresent() &&
                !userRepository.findByEmail(userUpdateRequest.email()).get().getId().equals(id)) {
            throw new EmailAlreadyExistsException("El correo ya está registrado");
        }

        // Actualizar los datos del usuario
        user.setEmail(userUpdateRequest.email());
        user.setFullName(userUpdateRequest.fullName());
        user.setCityOfResidence(userUpdateRequest.cityOfResidence());
        user.setDateBirth(userUpdateRequest.dateBirth());

        // Guardar el usuario actualizado
        User updatedUser = userRepository.save(user);
        log.info("Usuario actualizado exitosamente: {}", updatedUser.getEmail());

        // Convertir a UserResponse
        return mapToUserResponse(user);
    }

    @Transactional
    public SuccessResponse updateUserPassword(String id, PasswordUpdate passwordUpdate) {
        // Buscar el usuario por ID
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));

        // Verificar si la contraseña actual es correcta
        if (!passwordEncoder.matches(passwordUpdate.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("La contraseña actual es incorrecta");
        }

        // Actualizar la contraseña
        user.setPassword(passwordEncoder.encode(passwordUpdate.newPassword()));

        // Guardar el usuario actualizado
        userRepository.save(user);
        log.info("Contraseña actualizada exitosamente para el usuario: {}", user.getEmail());

        // Retornar la respuesta de éxito
        return new SuccessResponse("Contraseña actualizada exitosamente");
    }

    @Transactional
    public SuccessResponse deleteUser(String id){
        // Buscar el usuario por ID
        log.warn("Intentando eliminar usuario con el id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        user.setAccountStatus(AccountStatus.DELETED);
        userRepository.save(user);
        log.warn("Usuario con el id: {} eliminado exitosamente", id);
        return new SuccessResponse("Usuario eliminado exitosamente");
    }

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getDateBirth(), user.getAccountStatus().toString(),
                user.getCityOfResidence()
        );
    }


}