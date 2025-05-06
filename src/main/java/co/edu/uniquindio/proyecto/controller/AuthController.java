package co.edu.uniquindio.proyecto.controller;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.proyecto.dto.response.SuccessResponse;
import co.edu.uniquindio.proyecto.dto.user.JwtAccessResponse;
import co.edu.uniquindio.proyecto.dto.user.JwtResponse;
import co.edu.uniquindio.proyecto.dto.user.LoginRequest;
import co.edu.uniquindio.proyecto.dto.user.PasswordResetRequest;
import co.edu.uniquindio.proyecto.entity.auth.VerificationCodeType;
import co.edu.uniquindio.proyecto.service.interfaces.AuthService;
import co.edu.uniquindio.proyecto.service.interfaces.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
   * @param email ID del usuario que solicita un nuevo código.
   * @return HTTP 204 si el código fue enviado exitosamente.
   */
  @PostMapping("/activations/{email}")
  public ResponseEntity<Void> sendCodeAgain(@PathVariable String email) {
    log.info("📩 Reenviando código de activación al usuario con ID: {}", email);
    verificationService.resendCode(email, VerificationCodeType.ACTIVATION);
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
    log.info("🔐 Iniciando sesión para el usuario: {}", request.userName());

    // Ejecutar la autenticación y obtener los tokens
    JwtResponse jwtResponse = authService.authenticate(request);
    log.info("✅ Autenticación exitosa para el usuario: {}", request.userName());

    // Crear cookies para access y refresh
        ResponseCookie accessTokenCookie  = buildCookie("access_token",  jwtResponse.token(),        Duration.ofHours(1));
    ResponseCookie refreshTokenCookie = buildCookie("refresh_token", jwtResponse.refreshToken(), Duration.ofDays(7));
    log.debug("Cookies generadas: access_token ({}s), refresh_token ({}s)",
        accessTokenCookie.getMaxAge(), refreshTokenCookie.getMaxAge());

    // Devolver respuesta con headers Set-Cookie
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
        .body(jwtResponse);
  }


  @PostMapping("/users/password-codes/{email}")
  public ResponseEntity<Void> resendPasswordResetCode(@PathVariable String email) {
    log.info("📩 Reenviando código de activación al usuario con ID: {}", email);
    verificationService.resendCode(email, VerificationCodeType.PASSWORD_RESET);
    return ResponseEntity.noContent().build();
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
  @PatchMapping("/users/password")
  public ResponseEntity<SuccessResponse> confirmReset(@Valid @RequestBody PasswordResetRequest request) {
    log.info("🔄 Confirmando restablecimiento de contraseña con código: {}", request.code());
    verificationService.resetPasswordWithCode(request.code(), request.newPassword());
    return ResponseEntity.ok(new SuccessResponse("Contraseña actualizada exitosamente"));
  }

  /**
   * Endpoint para refrescar el token de acceso mediante refresh token.
   *
   * @param refreshToken Objeto que contiene el refresh token.
   * @return Un {@link JwtResponse} con el nuevo token de acceso.
   */
  @PostMapping("/accessTokens")
  public ResponseEntity<JwtAccessResponse> refreshToken(
      @CookieValue(name = "refresh_token", required = true) String refreshToken) {

    log.info("Recibida solicitud de refresco de token.");

    JwtAccessResponse response = authService.refreshAccessToken(refreshToken);
    return ResponseEntity.ok(response);
  }


    @GetMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    authService.logout(request);

    // Create cookies with maxAge set to 0 to delete them
    // Crear cookies para access y refresh
    ResponseCookie accessTokenCookie = buildCookie("access_token", "", Duration.ofHours(0));
    ResponseCookie refreshTokenCookie = buildCookie("refresh_token", "", Duration.ofDays(0));


    // Return response with Set-Cookie headers to delete cookies
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
        .build();
  }

  /**
   * Construye una {@link ResponseCookie} con las siguientes propiedades:
   * <ul>
   *   <li>HttpOnly: true</li>
   *   <li>Secure: true</li>
   *   <li>Path: "/"</li>
   *   <li>SameSite: Strict</li>
   *   <li>Max-Age: según parámetro</li>
   * </ul>
   *
   * @param name Nombre de la cookie.
   * @param value Valor de la cookie.
   * @param maxAge Duración de la cookie.
   * @return ResponseCookie ya configurada.
   */
  private ResponseCookie buildCookie(String name, String value, Duration maxAge) {
    log.debug("🔧 Construyendo cookie '{}', duración {} segundos", name, maxAge.getSeconds());
    return ResponseCookie.from(name, value)
        .httpOnly(true)
        //.secure(true)
        .path("/")
        .sameSite("Strict")
        .maxAge(maxAge)
        .build();
  }


}

