package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.user.JwtResponse;
import co.edu.uniquindio.proyecto.dto.user.LoginRequest;
import co.edu.uniquindio.proyecto.service.auth.VerificationService;
import co.edu.uniquindio.proyecto.service.implementations.AuthServiceImplements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación y verificación de cuentas.
 * Proporciona endpoints para la verificación de cuentas, reenvío de códigos de validación y inicio de sesión.
 */
@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final VerificationService verificationService;
    private final AuthServiceImplements authService;

    /**
     * Verifica la cuenta de un usuario a partir de un código de validación.
     *
     * @param code Código de verificación enviado al correo del usuario.
     * @return Respuesta con un mensaje de éxito si la cuenta se verifica correctamente.
     */
    @PatchMapping("/activations")
    public ResponseEntity<String> verifyAccount(@RequestParam String code) {
        log.info("Solicitud de verificación de cuenta con código: {}", code);
        verificationService.validateCode(code);
        return ResponseEntity.ok("Cuenta verificada exitosamente");
    }

    /**
     * Reenvía el código de verificación al usuario indicado.
     *
     * @param userId Identificador del usuario al que se debe reenviar el código.
     * @return Respuesta sin contenido (HTTP 204) indicando que la solicitud fue procesada.
     */
    @PostMapping("activations/{userId}")
    public ResponseEntity<Void> sendCodeAgain(@PathVariable String userId) {
        log.info("Reenviando código de validación al usuario con ID: {}", userId);
        verificationService.resendCode(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Inicia sesión en la aplicación.
     *
     * @param request Objeto que contiene las credenciales de inicio de sesión.
     * @return Respuesta con el token JWT si la autenticación es exitosa.
     */
    @PostMapping("/sessions")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Solicitud de inicio de sesión para el usuario: {}", request.userName());
        JwtResponse jwtResponse = authService.authenticate(request);
        log.info("Inicio de sesión exitoso para el usuario: {}", request.userName());
        return ResponseEntity.ok(jwtResponse);
    }
}
