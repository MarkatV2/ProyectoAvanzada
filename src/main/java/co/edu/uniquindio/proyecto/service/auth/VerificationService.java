package co.edu.uniquindio.proyecto.service.auth;

import co.edu.uniquindio.proyecto.entity.AccountStatus;
import co.edu.uniquindio.proyecto.entity.User;
import co.edu.uniquindio.proyecto.entity.VerificationToken;
import co.edu.uniquindio.proyecto.exception.InvalidTokenException;
import co.edu.uniquindio.proyecto.exception.TokenExpiredException;
import co.edu.uniquindio.proyecto.exception.UserNotFoundException;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.repository.VerificationTokenRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final VerificationTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    // Generar token y enviar correo
    public void generateAndSendVerificationToken(User user) {
        log.info("Iniciando la generación del token para el usuario con email: {}", user.getEmail());

        String token = jwtService.generateToken(user.getEmail());
        log.debug("Token generado: {}", token);

        LocalDateTime expiration = LocalDateTime.now().plusMinutes(15);
        log.debug("Fecha de expiración del token: {}", expiration);

        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .userId(user.getId())
                .expirationDate(expiration)
                .build();

        tokenRepository.save(verificationToken);
        log.info("Token guardado en la base de datos para el usuario: {}", user.getEmail());

        sendVerificationEmail(user.getEmail(), token);
        log.info("Correo de verificación enviado a: {}", user.getEmail());
    }

    // Enviar correo electrónico
    private void sendVerificationEmail(String toEmail, String token) {
        log.info("Preparando correo de verificación para: {}", toEmail);

        String verificationUrl = "http://localhost:8080/auth/sessions?token=" + token;
        log.debug("URL de verificación generada: {}", verificationUrl);

        String htmlContent = """
            <h1>Verifica tu cuenta</h1>
            <p>Haz clic en el botón para completar la verificación:</p>
            <a href="%s" style="padding: 10px; background-color: #4CAF50; color: white; text-decoration: none;">
                Validar cuenta
            </a>
            """.formatted(verificationUrl);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

        try {
            helper.setTo(toEmail);
            helper.setSubject("Verifica tu cuenta");
            helper.setText(htmlContent, true);
            log.info("Correo configurado correctamente para: {}", toEmail);

            mailSender.send(message);
            log.info("Correo enviado exitosamente a: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error al enviar el correo a: {}", toEmail, e);
            throw new RuntimeException("Error al enviar el correo", e);
        }
    }

    @Transactional
    public void verifyToken(String token) {
        log.info("Iniciando verificación del token");

        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Token inválido: {}", token);
                    return new InvalidTokenException("Token inválido");
                });

        log.debug("Token encontrado: {}, expiración: {}", token, verificationToken.getExpirationDate());

        if (LocalDateTime.now().isAfter(verificationToken.getExpirationDate())) {
            log.warn("El token ha expirado: {}", token);
            throw new TokenExpiredException("El token ha expirado");
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado para el token: {}", token);
                    return new UserNotFoundException("Usuario no encontrado");
                });

        user.setAccountStatus(AccountStatus.ACTIVATED);
        userRepository.save(user);
        log.info("Cuenta activada para el usuario con ID: {}", user.getId());

        tokenRepository.delete(verificationToken);
        log.info("Token eliminado tras la activación: {}", token);
    }


}
