package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.response.SuccessResponse;
import co.edu.uniquindio.proyecto.dto.user.*;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exception.user.EmailAlreadyExistsException;
import co.edu.uniquindio.proyecto.exception.user.InvalidPasswordException;
import co.edu.uniquindio.proyecto.exception.global.ServiceUnavailableException;
import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.service.auth.VerificationService;
import co.edu.uniquindio.proyecto.service.interfaces.UserService;
import co.edu.uniquindio.proyecto.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
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

    /**
     * Recupera una lista paginada de usuarios.
     *
     * @param page número de página (comienza en 1)
     * @param size cantidad de usuarios por página (máximo 100)
     * @return objeto {@code PaginatedUserResponse} con la lista y datos de paginación
     */
    public PaginatedUserResponse getUsers(int page, int size) {
        log.info("Solicitando lista de usuarios. Página: {}, Tamaño: {}", page, size);
        try {
            // Ajuste de parámetros
            page = Math.max(page, 1);
            size = Math.min(Math.max(size, 1), 100);
            log.info("Parámetros ajustados. Página: {}, Tamaño: {}", page, size);

            Pageable pageable = PageRequest.of(page - 1, size);
            Page<User> userPage = userRepository.findAll(pageable);
            log.info("Usuarios recuperados: {}. Total de páginas: {}",
                    userPage.getTotalElements(), userPage.getTotalPages());

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

    /**
     * Registra un nuevo usuario.
     *
     * @param userRegistration datos de registro del usuario
     * @return {@code UserResponse} con la información del usuario registrado
     * @throws EmailAlreadyExistsException si el correo ya está registrado
     */
    @Transactional
    public UserResponse registerUser(UserRegistration userRegistration) {
        log.info("Consultando usuario con email: {} ...", userRegistration.email());
        if (userRepository.findByEmail(userRegistration.email()).isPresent()) {
            log.info("El correo {} ya existe", userRegistration.email());
            throw new EmailAlreadyExistsException("El correo ya está registrado");
        }

        log.info("Registrando usuario: {}", userRegistration.email());
        User user = userMapper.toUserEntity(userRegistration);
        try {
            User savedUser = userRepository.save(user);
            log.info("Usuario registrado exitosamente: {}", savedUser.getEmail());
            log.info("Generando token de validación para el usuario: {}", savedUser.getEmail());
            verificationService.generateAndSendCode(savedUser);
            return userMapper.toUserResponse(savedUser);
        } catch (DataAccessException e) {
            log.error("Error al registrar el usuario: {}", e.getMessage(), e);
            throw new ServiceUnavailableException("Error al acceder a la base de datos");
        }
    }

    /**
     * Consulta la información de un usuario por su ID.
     *
     * @param userId ID del usuario a consultar
     * @return {@code UserResponse} con la información del usuario
     * @throws UserNotFoundException si el usuario no existe
     */
    public UserResponse getUser(String userId) {
        log.info("Consultando usuario con ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        return userMapper.toUserResponse(user);
    }

    /**
     * Actualiza los datos de un usuario.
     *
     * @param id                ID del usuario a actualizar
     * @param userUpdateRequest datos actualizados del usuario
     * @return {@code UserResponse} con la información actualizada
     * @throws UserNotFoundException        si el usuario no existe
     * @throws EmailAlreadyExistsException  si el correo actualizado ya está registrado en otro usuario
     */
    @Transactional
    public UserResponse updateUser(String id, UserUpdateRequest userUpdateRequest) {
        log.info("Consultando usuario con ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));

        log.info("Verificando disponibilidad del correo: {}", userUpdateRequest.email());
        var emailUserOpt = userRepository.findByEmail(userUpdateRequest.email());
        if (emailUserOpt.isPresent() && !emailUserOpt.get().getId().equals(id)) {
            log.info("El correo {} ya existe", userUpdateRequest.email());
            throw new EmailAlreadyExistsException("El correo ya está registrado");
        }

        log.info("Actualizando datos del usuario con correo: {}", userUpdateRequest.email());
        userMapper.updateUserFromRequest(userUpdateRequest, user);
        User updatedUser = userRepository.save(user);
        log.info("Usuario actualizado exitosamente: {}", updatedUser.getEmail());
        return userMapper.toUserResponse(updatedUser);
    }

    /**
     * Actualiza la contraseña de un usuario.
     *
     * @param id             ID del usuario
     * @param passwordUpdate datos para la actualización de contraseña
     * @return {@code SuccessResponse} confirmando la actualización
     * @throws UserNotFoundException    si el usuario no existe
     * @throws InvalidPasswordException si la contraseña actual es incorrecta
     */
    @Transactional
    public SuccessResponse updateUserPassword(String id, PasswordUpdate passwordUpdate) {
        log.info("Consultando usuario con ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));

        if (!passwordEncoder.matches(passwordUpdate.currentPassword(), user.getPassword())) {
            log.info("Contraseña actual incorrecta para el usuario: {}", user.getEmail());
            throw new InvalidPasswordException("La contraseña actual es incorrecta");
        }

        log.info("Actualizando contraseña para el usuario: {}", user.getEmail());
        user.setPassword(passwordEncoder.encode(passwordUpdate.newPassword()));
        userRepository.save(user);
        log.info("Contraseña actualizada exitosamente para el usuario: {}", user.getEmail());
        return new SuccessResponse("Contraseña actualizada exitosamente");
    }

    /**
     * Elimina lógicamente un usuario marcando su estado de cuenta como DELETED.
     *
     * @param id ID del usuario a eliminar
     * @return {@code SuccessResponse} confirmando la eliminación
     * @throws UserNotFoundException si el usuario no existe
     */
    @Transactional
    public SuccessResponse deleteUser(String id) {
        log.info("Eliminando usuario con ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        user.setAccountStatus(AccountStatus.DELETED);
        userRepository.save(user);
        log.info("Usuario con ID: {} eliminado exitosamente", id);
        return new SuccessResponse("Usuario eliminado exitosamente");
    }
}
