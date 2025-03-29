package co.edu.uniquindio.proyecto.service.auth;

import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.entity.auth.VerificationCode;
import co.edu.uniquindio.proyecto.exception.auth.InvalidCodeException;
import co.edu.uniquindio.proyecto.exception.auth.CodeExpiredException;
import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.repository.VerificationCodeRepository;
import co.edu.uniquindio.proyecto.service.mapper.VerificationCodeMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Servicio encargado de la generación, envío y validación de códigos de verificación para la activación de cuentas.
 */
@Service
@Slf4j
public class VerificationService {

    private final VerificationCodeRepository codeRepository;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final VerificationCodeMapper verificationCodeMapper;
    private final String verificationTemplate;
    private static final int EXPIRATION_MINUTES = 15;

    /**
     * Constructor de VerificationService.
     *
     * @param codeRepository       Repositorio para el manejo de códigos de verificación.
     * @param mailSender           Servicio de envío de correos.
     * @param userRepository       Repositorio de usuarios.
     * @param verificationCodeMapper Mapper para transformar datos a entidades de VerificationCode.
     * @param verificationTemplate Plantilla de correo para la verificación.
     */
    @Autowired
    public VerificationService(
            VerificationCodeRepository codeRepository,
            JavaMailSender mailSender,
            UserRepository userRepository,
            VerificationCodeMapper verificationCodeMapper,
            @Value("#{@verificationEmailTemplate}") String verificationTemplate
    ) {
        this.codeRepository = codeRepository;
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.verificationCodeMapper = verificationCodeMapper;
        this.verificationTemplate = verificationTemplate;
    }

    /**
     * Genera un código de verificación y lo envía al correo del usuario.
     *
     * @param user Usuario al que se le enviará el código.
     */
    public void generateAndSendCode(User user) {
        String code = generateRandomCode();
        VerificationCode verificationCode = verificationCodeMapper.toVerificationCode(code, user, EXPIRATION_MINUTES);

        codeRepository.save(verificationCode);
        log.info("Código de verificación generado y almacenado para el usuario: {}", user.getEmail());
        sendVerificationEmail(user.getEmail(), code);
    }

    /**
     * Genera un código aleatorio de 6 dígitos.
     *
     * @return Código generado.
     */
    private String generateRandomCode() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(999999));
    }

    /**
     * Valida que el código de verificación exista y no haya expirado.
     * Si es válido, activa la cuenta del usuario asociado.
     *
     * @param code Código de verificación a validar.
     * @throws InvalidCodeException si el código no existe.
     * @throws CodeExpiredException si el código ha expirado.
     */
    public void validateCode(String code) {
        log.info("Iniciando validación del código: {}", code);
        VerificationCode verificationCode = codeRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.warn("Código inválido: {}", code);
                    return new InvalidCodeException("Código inválido");
                });

        log.debug("Código encontrado: {}. Expiración: {}", code, verificationCode.getExpiresAt());
        if (LocalDateTime.now().isAfter(verificationCode.getExpiresAt())) {
            log.warn("El código ha expirado: {}", code);
            codeRepository.delete(verificationCode);
            throw new CodeExpiredException("El código ha expirado");
        }
        validateUserAccount(verificationCode);
    }

    /**
     * Activa la cuenta del usuario asociado al código y elimina el código tras la validación.
     *
     * @param verificationCode Código de verificación validado.
     * @throws UserNotFoundException si no se encuentra el usuario asociado.
     */
    private void validateUserAccount(VerificationCode verificationCode) {
        User user = userRepository.findById(verificationCode.getUserId())
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado para el código: {}", verificationCode.getCode());
                    return new UserNotFoundException("Usuario no encontrado");
                });
        user.setAccountStatus(AccountStatus.ACTIVATED);
        userRepository.save(user);
        log.info("Cuenta activada para el usuario con ID: {}", user.getId());
        codeRepository.delete(verificationCode);
        log.info("Código eliminado tras la activación: {}", verificationCode.getCode());
    }

    /**
     * Envía un correo de verificación al usuario.
     *
     * @param toEmail Dirección de correo del destinatario.
     * @param code    Código de verificación.
     */
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

    /**
     * Construye el contenido HTML del correo de verificación.
     *
     * @param email Dirección de correo.
     * @param code  Código de verificación.
     * @return Contenido HTML preparado.
     */
    private String buildEmailContent(String email, String code) {
        return verificationTemplate
                .replace("{{email}}", email)
                .replace("{{code}}", code);
    }

    /**
     * Reenvía un código de verificación. Elimina los códigos previos generados para el usuario.
     *
     * @param userId Identificador del usuario (en formato ObjectId).
     * @throws UserNotFoundException si no se encuentra el usuario.
     */
    public void resendCode(String userId) {
        // Convertir el String a ObjectId; se asume que el userId es válido.
        ObjectId objectId = new ObjectId(userId);
        User user = userRepository.findById(objectId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        codeRepository.deleteAllByUserId(objectId);
        log.info("Códigos previos eliminados para el usuario con ID: {}", userId);
        generateAndSendCode(user);
    }
}
