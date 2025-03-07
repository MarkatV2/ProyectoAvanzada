package co.edu.uniquindio.proyecto.Controller;

// UserController.java
import co.edu.uniquindio.proyecto.DTO.*;
import co.edu.uniquindio.proyecto.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.naming.ServiceUnavailableException;
import java.net.URI;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

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
    @Operation(
            summary = "Registrar un nuevo usuario",
            description = "Registra un nuevo usuario en el sistema",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
                    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
                    @ApiResponse(responseCode = "409", description = "Correo ya registrado"),
                    @ApiResponse(responseCode = "500", description = "Error interno del servidor"),
                    @ApiResponse(responseCode = "503", description = "Servicio no disponible")
            }
    )
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistration userRegistration) {
        log.info("Registrando nuevo usuario: {}", userRegistration.email());
        UserResponse response = userService.registerUser(userRegistration);

        // Crear la URI del recurso creado
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        // Retornar la respuesta con el encabezado Location
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, location.toString())
                .body(response);
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
        SuccessResponse response = userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}