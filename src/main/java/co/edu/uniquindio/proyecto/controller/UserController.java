package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.response.SuccessResponse;
import co.edu.uniquindio.proyecto.dto.user.*;
import co.edu.uniquindio.proyecto.service.interfaces.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Controlador REST para la gestión de usuarios.
 * <p>
 * Proporciona endpoints para listar, registrar, consultar, actualizar y eliminar usuarios.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Recupera una lista paginada de usuarios.
     * Solo accesible por administradores.
     *
     * @param page Número de página (mínimo 1).
     * @param size Tamaño de la página (mínimo 1 y máximo 100).
     * @return Lista paginada de usuarios.
     */
    @GetMapping
    public ResponseEntity<PaginatedUserResponse> getUsers(
            @RequestParam(defaultValue = "1") @Positive(message = "La página debe ser un número positivo") int page,
            @RequestParam(defaultValue = "30") @Positive(message = "El tamaño debe ser un número positivo") int size) {

        log.info("📋 Consultando usuarios - Página: {}, Tamaño: {}", page, size);
        PaginatedUserResponse response = userService.getUsers(page, size);
        log.info("✅ Total de usuarios recuperados: {}", response.totalItems());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> consultUser(){
        UserResponse user = userService.getCurrentUser();
        return ResponseEntity.ok(user);
    }


    /**
     * Registra un nuevo usuario.
     *
     * @param userRegistration Datos del usuario.
     * @return Usuario registrado con URI de acceso.
     */
    @PostMapping
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistration userRegistration) {
        log.info("🆕 Registrando nuevo usuario: {}", userRegistration.email());
        UserResponse response = userService.registerUser(userRegistration);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        log.info("✅ Usuario registrado con éxito: {}", response.email());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Consulta la información de un usuario por su ID.
     * Permitido al propio usuario o a administradores.
     *
     * @param userId ID del usuario.
     * @return Datos del usuario.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        log.info("🔎 Consultando usuario con ID: {}", userId);
        UserResponse response = userService.getUser(userId);
        log.info("✅ Usuario encontrado: {}", response.email());
        return ResponseEntity.ok(response);
    }

    /**
     * Actualiza los datos personales del usuario.
     *
     * @param id                ID del usuario.
     * @param userUpdateRequest Datos nuevos.
     * @return Usuario actualizado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UserUpdateRequest userUpdateRequest) {

        log.info("✏️ Actualizando usuario con ID: {}", id);
        UserResponse response = userService.updateUser(id, userUpdateRequest);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .build()
                .toUri();

        log.info("✅ Usuario actualizado: {}", response.email());
        return ResponseEntity.ok()
                .header(HttpHeaders.LOCATION, location.toString())
                .body(response);
    }

    /**
     * Actualiza la contraseña del usuario autenticado.
     *
     * @param id             ID del usuario.
     * @param passwordUpdate Nueva contraseña.
     * @return Confirmación de actualización.
     */
    @PatchMapping("/{id}/password")
    public ResponseEntity<SuccessResponse> updateUserPassword(
            @PathVariable String id,
            @Valid @RequestBody PasswordUpdate passwordUpdate) {
        log.info("🔐 Actualizando contraseña para usuario: {}", id);
        SuccessResponse response = userService.updateUserPassword(id, passwordUpdate);
        log.info("✅ Contraseña actualizada exitosamente para usuario: {}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina lógicamente la cuenta de un usuario.
     *
     * @param id ID del usuario.
     * @return Sin contenido si fue exitoso.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        log.info("🗑️ Eliminando usuario con ID: {}", id);
        userService.deleteUser(id);
        log.info("✅ Usuario con ID: {} eliminado correctamente", id);
        return ResponseEntity.noContent().build();
    }
}