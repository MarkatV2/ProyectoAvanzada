package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.response.SuccessResponse;
import co.edu.uniquindio.proyecto.dto.user.JwtResponse;
import co.edu.uniquindio.proyecto.dto.user.LoginRequest;
import co.edu.uniquindio.proyecto.dto.user.PasswordResetRequest;
import co.edu.uniquindio.proyecto.entity.auth.VerificationCodeType;
import co.edu.uniquindio.proyecto.service.interfaces.AuthService;
import co.edu.uniquindio.proyecto.service.interfaces.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador que gestiona los endpoints relacionados con autenticación,
 * verificación de cuentas y recuperación de contraseñas.
 */
@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final VerificationService verificationService;
    private final AuthService authService;

    /**
     * Verifica la cuenta del usuario mediante el código recibido por correo electrónico.
     *
     * @param code Código de verificación enviado previamente al correo del usuario.
     * @return Respuesta con mensaje de éxito si el código es válido.
     */
    @PatchMapping("/activations")
    public ResponseEntity<String> verifyAccount(@RequestParam String code) {
        log.info("🛂 Verificando cuenta con código: {}", code);
        verificationService.validateCodeActivation(code);
        return ResponseEntity.ok("Cuenta verificada exitosamente");
    }

    /**
     * Reenvía un nuevo código de activación al usuario especificado por su ID.
     *
     * @param userId ID del usuario que solicita un nuevo código.
     * @return HTTP 204 si el código fue enviado exitosamente.
     */
    @PostMapping("/activations/{userId}")
    public ResponseEntity<Void> sendCodeAgain(@PathVariable String userId) {
        log.info("📩 Reenviando código de activación al usuario con ID: {}", userId);
        verificationService.resendCode(userId, VerificationCodeType.ACTIVATION);
        return ResponseEntity.noContent().build();
    }

    /**
     * Realiza el proceso de autenticación del usuario, devolviendo un token JWT si es exitoso.
     *
     * @param request Objeto con las credenciales de inicio de sesión.
     * @return JWT válido para sesiones autenticadas.
     */
    @PostMapping("/sessions")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("🔐 Solicitud de inicio de sesión para el usuario: {}", request.userName());
        JwtResponse jwtResponse = authService.authenticate(request);
        log.info("✅ Inicio de sesión exitoso para el usuario: {}", request.userName());
        return ResponseEntity.ok(jwtResponse);
    }

    /**
     * Solicita un código de recuperación de contraseña para el correo electrónico dado.
     *
     * @param email Correo del usuario que desea recuperar su contraseña.
     * @return HTTP 204 si el correo fue enviado correctamente.
     */
    @PostMapping("/passwordCodes")
    public ResponseEntity<Void> requestPasswordReset(@RequestParam String email) {
        log.info("🔁 Solicitud de código de recuperación para: {}", email);
        verificationService.sendPasswordResetCode(email);
        return ResponseEntity.noContent().build();
    }

    /**
     * Confirma la recuperación de contraseña usando un código enviado por correo electrónico.
     *
     * @param request DTO que contiene el código de validación y la nueva contraseña.
     * @return Respuesta indicando que la contraseña fue actualizada exitosamente.
     */
    @PatchMapping("/password")
    public ResponseEntity<SuccessResponse> confirmReset(@Valid @RequestBody PasswordResetRequest request) {
        log.info("🔄 Confirmando restablecimiento de contraseña con código: {}", request.code());
        verificationService.resetPasswordWithCode(request.code(), request.newPassword());
        return ResponseEntity.ok(new SuccessResponse("Contraseña actualizada exitosamente"));
    }
}