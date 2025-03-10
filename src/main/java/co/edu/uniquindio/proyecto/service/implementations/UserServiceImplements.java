package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.*;
import co.edu.uniquindio.proyecto.entity.AccountStatus;
import co.edu.uniquindio.proyecto.entity.User;
import co.edu.uniquindio.proyecto.exception.EmailAlreadyExistsException;
import co.edu.uniquindio.proyecto.exception.InvalidPasswordException;
import co.edu.uniquindio.proyecto.exception.ServiceUnavailableException;
import co.edu.uniquindio.proyecto.exception.UserNotFoundException;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.service.auth.VerificationService;
import co.edu.uniquindio.proyecto.service.interfaces.UserService;
import co.edu.uniquindio.proyecto.service.mapper.UserMapper;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImplements implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final VerificationService verificationService;

    public PaginatedUserResponse getUsers(int page, int size) {
        log.info("Solicitando lista de usuarios. Página: {}, Tamaño: {}", page, size);
        try {
            // Validar y ajustar parámetros
            page = Math.max(page, 1);
            size = Math.min(Math.max(size, 1), 100);
            log.debug("Parámetros ajustados. Página: {}, Tamaño: {}", page, size);
            Pageable pageable = PageRequest.of(page - 1, size);
            Page<User> userPage = userRepository.findAll(pageable);
            log.info("Usuarios recuperados: {}. Total páginas: {}", userPage.getTotalElements(), userPage.getTotalPages());

            return new PaginatedUserResponse(
                    (int) userPage.getTotalElements(),
                    userPage.getTotalPages(),
                    page,
                    userPage.getContent().stream()
                            .map(userMapper::toUserResponse)
                            .collect(Collectors.toList())
            );
        } catch (UncategorizedMongoDbException e) {
            log.error("Error de MongoDB al obtener usuarios: {}", e.getMessage(), e);
            throw new ServiceUnavailableException("Error al acceder a la base de datos");
        } catch (DataAccessException e) {
            log.error("Error de acceso a datos al obtener usuarios: {}", e.getMessage(), e);
            throw new ServiceUnavailableException("Error de acceso a datos");
        }
    }
    @Transactional
    public UserResponse registerUser(UserRegistration userRegistration) {
        log.info("Consultando usuario con email: {} ...", userRegistration.email());
        // Verificar si el correo ya está registrado
        if (userRepository.findByEmail(userRegistration.email()).isPresent()) {
            log.warn("El correo : {} ya existe", userRegistration.email());
            throw new EmailAlreadyExistsException("El correo ya está registrado");
        }

        log.info("Registrando usuario {} ....", userRegistration.email());
        // Crear el usuario
        log.info("usuario antes del mapper {} {}", userRegistration.email(), userRegistration.cityOfResidence());
        User user = userMapper.toUserEntity(userRegistration);
        log.info("usuario despues del mapper {} {}", user.getEmail(), user.getCityOfResidence());

        try {
            // Guardar el usuario en la base de datos
            User savedUser = userRepository.save(user);
            log.debug("Usuario registrado exitosamente: {}", savedUser.getEmail());
            log.info("Generando token de validación para el usuario: {} ...", savedUser.getEmail());
            verificationService.generateAndSendVerificationToken(savedUser);
            // Convertir a UserResponse
            return userMapper.toUserResponse(savedUser);
        } catch (DataAccessException e) {
            log.error("Error al registrar el usuario: {}", e.getMessage());
            throw new ServiceUnavailableException("Error al acceder a la base de datos");
        }
    }

    public UserResponse getUser(String userId) {
        // Buscar el usuario por ID
        log.info("Consultando Usuario con ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        // Convertir a UserResponse
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(String id, UserUpdateRequest userUpdateRequest) {
        // Buscar el usuario por ID
        log.info("Consultando Usuario con ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));

        // Verificar si el correo ya está registrado por otro usuario
        log.info("Consultando usuario con email: {} ...", userUpdateRequest.email());
        if (userRepository.findByEmail(userUpdateRequest.email()).isPresent() &&
                !userRepository.findByEmail(userUpdateRequest.email()).get().getId().equals(id)) {
            log.warn("El correo : {} ya existe", userUpdateRequest.email());
            throw new EmailAlreadyExistsException("El correo ya está registrado");
        }

        // Actualizar los datos del usuario
        log.info("Actualizando usuario {}", userUpdateRequest.email());
        userMapper.updateUserFromRequest(userUpdateRequest, user);
        // Guardar el usuario actualizado
        User updatedUser = userRepository.save(user);
        log.debug("Usuario actualizado exitosamente: {}", updatedUser.getEmail());

        // Convertir a UserResponse
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public SuccessResponse updateUserPassword(String id, PasswordUpdate passwordUpdate) {
        // Buscar el usuario por ID
        log.info("Consultando Usuario con ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));

        // Verificar si la contraseña actual es correcta
        if (!passwordEncoder.matches(passwordUpdate.currentPassword(), user.getPassword())) {
            log.warn("El usuario {} ingreso mal la contraseña", user.getEmail());
            throw new InvalidPasswordException("La contraseña actual es incorrecta");
        }

        // Actualizar la contraseña
        log.warn("Actualizando contraseña al usuario {} ....", user.getEmail());
        user.setPassword(passwordEncoder.encode(passwordUpdate.newPassword()));

        // Guardar el usuario actualizado
        userRepository.save(user);
        log.debug("Contraseña actualizada exitosamente para el usuario: {}", user.getEmail());

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

}