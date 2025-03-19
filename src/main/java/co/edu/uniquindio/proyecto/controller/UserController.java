package co.edu.uniquindio.proyecto.controller;

// UserController.java
import co.edu.uniquindio.proyecto.dto.response.SuccessResponse;
import co.edu.uniquindio.proyecto.dto.user.*;
import co.edu.uniquindio.proyecto.service.implementations.UserServiceImplements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserServiceImplements userService;

    @GetMapping
    public ResponseEntity<PaginatedUserResponse> getUsers(
            @RequestParam(defaultValue = "1") @Positive(message = "La página debe ser un número positivo") int page,
            @RequestParam(defaultValue = "30") @Positive(message = "El tamaño debe ser un número positivo") int size){

        log.info("Consultando usuarios - Página: {}, Tamaño: {}", page, size);
        PaginatedUserResponse response = userService.getUsers(page, size);
        log.debug("Consulta exitosa. Total usuarios: {}", response.totalItems());
        return ResponseEntity.ok(response);
    }


    @PostMapping
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistration userRegistration) {
        log.info("Registrando nuevo usuario: {}", userRegistration.email());
        UserResponse response = userService.registerUser(userRegistration);

        // Crear la URI del recurso creado
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        log.debug("Usuario creado exitosamente: {}", response.email());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        log.info("Consultando usuario con ID: {}", userId);
        UserResponse response = userService.getUser(userId);
        log.debug("Usuario encontrado: {}", response);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
        log.info("Actualizando usuario con ID: {}", id);
        UserResponse response = userService.updateUser(id, userUpdateRequest);
        log.debug("Usuario actualizado: {}", response);
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/{id}/password")
    public ResponseEntity<SuccessResponse> updateUserPassword(
            @PathVariable String id,
            @Valid @RequestBody PasswordUpdate passwordUpdate) {
        log.info("Actualizando contraseña del usuario con ID: {}", id);
        SuccessResponse response = userService.updateUserPassword(id, passwordUpdate);
        log.debug("Contraseña actualizada exitosamente para el usuario: {}", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id){
        log.info("Solicitud recibida para eliminar el usuario con el id: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}