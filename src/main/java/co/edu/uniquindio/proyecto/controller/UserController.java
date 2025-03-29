package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.response.SuccessResponse;
import co.edu.uniquindio.proyecto.dto.user.*;
import co.edu.uniquindio.proyecto.service.implementations.UserServiceImplements;
import co.edu.uniquindio.proyecto.target.CheckSelfOrAdminPermission;
import co.edu.uniquindio.proyecto.target.CheckSelfPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Controlador para la gestión de usuarios.
 * Proporciona endpoints para listar, registrar, consultar, actualizar y eliminar usuarios.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserServiceImplements userService;

    /**
     * Recupera una lista paginada de usuarios.
     *
     * @param page Número de página (mínimo 1).
     * @param size Tamaño de la página (mínimo 1 y máximo 100).
     * @return Respuesta con la lista de usuarios paginados.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaginatedUserResponse> getUsers(
            @RequestParam(defaultValue = "1") @Positive(message = "La página debe ser un número positivo") int page,
            @RequestParam(defaultValue = "30") @Positive(message = "El tamaño debe ser un número positivo") int size) {

        log.info("Consultando usuarios - Página: {}, Tamaño: {}", page, size);
        PaginatedUserResponse response = userService.getUsers(page, size);
        log.info("Consulta exitosa. Total usuarios: {}", response.totalItems());
        return ResponseEntity.ok(response);
    }

    /**
     * Registra un nuevo usuario.
     *
     * @param userRegistration Datos del usuario a registrar.
     * @return Respuesta con la información del usuario creado.
     */
    @PostMapping
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistration userRegistration) {
        log.info("Registrando nuevo usuario: {}", userRegistration.email());
        UserResponse response = userService.registerUser(userRegistration);

        // Construir la URI del recurso creado.
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        log.info("Usuario creado exitosamente: {}", response.email());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Consulta la información de un usuario por su ID.
     * Acceso permitido al propio usuario o a administradores.
     *
     * @param userId ID del usuario.
     * @return Respuesta con la información del usuario.
     */
    @GetMapping("/{userId}")
    @CheckSelfOrAdminPermission
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        log.info("Consultando usuario con ID: {}", userId);
        UserResponse response = userService.getUser(userId);
        log.info("Usuario encontrado: {}", response.email());
        return ResponseEntity.ok(response);
    }

    /**
     * Actualiza los datos de un usuario.
     * Solo puede ser ejecutado por el mismo usuario.
     *
     * @param id                ID del usuario a actualizar.
     * @param userUpdateRequest Datos de actualización.
     * @return Respuesta con la información actualizada del usuario.
     */
    @PutMapping("/{id}")
    @CheckSelfPermission
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
        log.info("Actualizando usuario con ID: {}", id);
        UserResponse response = userService.updateUser(id, userUpdateRequest);
        log.info("Usuario actualizado: {}", response.email());
        return ResponseEntity.ok(response);
    }

    /**
     * Actualiza la contraseña de un usuario.
     * Solo puede ser ejecutado por el mismo usuario.
     *
     * @param id             ID del usuario.
     * @param passwordUpdate Datos para la actualización de contraseña.
     * @return Respuesta de éxito.
     */
    @PatchMapping("/{id}/password")
    @CheckSelfPermission
    public ResponseEntity<SuccessResponse> updateUserPassword(
            @PathVariable String id,
            @Valid @RequestBody PasswordUpdate passwordUpdate) {
        log.info("Actualizando contraseña del usuario con ID: {}", id);
        SuccessResponse response = userService.updateUserPassword(id, passwordUpdate);
        log.info("Contraseña actualizada exitosamente para el usuario con ID: {}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina lógicamente un usuario.
     * Solo puede ser ejecutado por el mismo usuario.
     *
     * @param id ID del usuario a eliminar.
     * @return Respuesta sin contenido.
     */
    @DeleteMapping("/{id}")
    @CheckSelfPermission
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        log.info("Solicitud para eliminar el usuario con ID: {}", id);
        userService.deleteUser(id);
        log.info("Usuario con ID: {} eliminado exitosamente", id);
        return ResponseEntity.noContent().build();
    }
}
