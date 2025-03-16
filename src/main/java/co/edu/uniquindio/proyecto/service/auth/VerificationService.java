package co.edu.uniquindio.proyecto.service.auth;

import co.edu.uniquindio.proyecto.entity.AccountStatus;
import co.edu.uniquindio.proyecto.entity.User;
import co.edu.uniquindio.proyecto.entity.VerificationCode;
import co.edu.uniquindio.proyecto.exception.InvalidTokenException;
import co.edu.uniquindio.proyecto.exception.TokenExpiredException;
import co.edu.uniquindio.proyecto.exception.UserNotFoundException;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.repository.VerificationCodeRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Date;

@Service
@Slf4j
public class VerificationService {

    private final VerificationCodeRepository codeRepository;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final String verificationTemplate;
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 15;

    @Autowired
    public VerificationService(
            VerificationCodeRepository codeRepository, JavaMailSender mailSender,
            UserRepository userRepository,
            JwtService jwtService,
            @Value("#{@verificationEmailTemplate}") String verificationTemplate
    ) {
        this.codeRepository = codeRepository;
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.verificationTemplate = verificationTemplate;
    }


    public void generateAndSendCode(User user) {
        String code = generateRandomCode();

        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setCode(code);
        verificationCode.setUserId(user.getId());
        verificationCode.setCreatedAt(LocalDateTime.now());
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES));

        codeRepository.save(verificationCode);
        sendVerificationEmail(user.getEmail(), code);
    }

    private String generateRandomCode() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(999999));
    }

    public void validateCode(String code) {
        log.info("Iniciando verificación del token");
        VerificationCode verificationCode = codeRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.warn("Código inválido: {}", code);
                    return new InvalidTokenException("Código inválido");
                });

        log.debug("Código encontrado: {}, expiración: {}", code, verificationCode.getExpiresAt());

        if (LocalDateTime.now().isAfter(verificationCode.getExpiresAt())) {
            log.warn("El token ha expirado: {}", code);
            throw new TokenExpiredException("El token ha expirado");
        }
        validateUserAccount(verificationCode);
    }

    private void validateUserAccount(VerificationCode verificationCode) {
        User user = userRepository.findById(verificationCode.getUserId())
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado para el token: {}", verificationCode.getCode());
                    return new UserNotFoundException("Usuario no encontrado");
                });
        user.setAccountStatus(AccountStatus.ACTIVATED);
        userRepository.save(user);
        log.info("Cuenta activada para el usuario con ID: {}", user.getId());

        codeRepository.delete(verificationCode);
        log.info("Token eliminado tras la activación: {}", verificationCode.getCode());
    }

    // Enviar correo electrónico
    private void sendVerificationEmail(String toEmail, String code) {
        log.info("Preparando correo de verificación para: {}", toEmail);

        try {
            String htmlContent = buildEmailContent(toEmail, code);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Verificación de cuenta requerida");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Correo enviado exitosamente a: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error al enviar el correo a: {}", toEmail, e);
            throw new RuntimeException("Error al enviar el correo", e);
        }
    }

    private String buildEmailContent(String email, String code) {
        return verificationTemplate
                .replace("{{email}}", email)
                .replace("{{code}}", code);
    }

}
