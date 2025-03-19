package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.service.auth.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final VerificationService verificationService;

    @PatchMapping("/activations")
    public ResponseEntity<String> verifyAccount(@RequestParam String code) {
        log.info("Solicitud de verificación de cuenta...");
        verificationService.validateCode(code);
        return ResponseEntity.ok("Cuenta verificada exitosamente");
    }

}
