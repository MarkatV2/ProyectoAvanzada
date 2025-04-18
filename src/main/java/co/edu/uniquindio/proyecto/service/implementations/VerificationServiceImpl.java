package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.entity.auth.VerificationCodeType;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.entity.auth.VerificationCode;
import co.edu.uniquindio.proyecto.exception.auth.InvalidCodeException;
import co.edu.uniquindio.proyecto.exception.auth.CodeExpiredException;
import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.repository.VerificationCodeRepository;
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.service.interfaces.VerificationService;
import co.edu.uniquindio.proyecto.service.mapper.VerificationCodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Servicio responsable de generar, enviar, validar y procesar códigos de verificación.
 * Se utiliza para activación de cuentas y recuperación de contraseñas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements VerificationService {

    private final VerificationCodeRepository codeRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final VerificationCodeMapper verificationCodeMapper;
    private final PasswordEncoder passwordEncoder;

    private static final int EXPIRATION_MINUTES = 15;

    /**
     * Genera un código de verificación y lo envía por correo electrónico al usuario,
     * dependiendo del tipo de verificación.
     *
     * @param user el usuario que recibirá el código.
     * @param type el tipo de verificación (ACTIVATION o PASSWORD_RESET).
     */
    @Override
    public void generateAndSendCode(User user, VerificationCodeType type) {
        String code = generateRandomCode();
        VerificationCode verificationCode = verificationCodeMapper.toVerificationCode(code, user, EXPIRATION_MINUTES);
        verificationCode.setVerificationCodeType(type);

        codeRepository.save(verificationCode);
        log.info("Código de verificación generado y guardado para el usuario: {} (tipo: {})", user.getEmail(), type);

        switch (type) {
            case ACTIVATION -> emailService.sendVerificationEmail(user.getEmail(), code);
            case PASSWORD_RESET -> emailService.sendPasswordResetEmail(user.getEmail(), code);
            default -> log.warn("Tipo de verificación no soportado: {}", type);
        }

        log.info("Correo enviado a {} para tipo de verificación {}", user.getEmail(), type);
    }


    /**
     * Valida el código de activación y activa la cuenta del usuario si es válido.
     *
     * @param code el código recibido por el usuario.
     * @throws InvalidCodeException si el código no existe.
     * @throws CodeExpiredException si el código ha expirado.
     */
    @Override
    public void validateCodeActivation(String code) {
        VerificationCode verificationCode = validateCode(code);
        validateUserAccount(verificationCode);
    }



    /**
     * Reenvía un nuevo código de verificación, eliminando códigos anteriores del usuario.
     *
     * @param userId ID del usuario al que se le reenviará el código.
     * @param type tipo de código a reenviar.
     * @throws UserNotFoundException si no se encuentra el usuario.
     */
    @Override
    public void resendCode(String userId, VerificationCodeType type) {
        ObjectId objectId = new ObjectId(userId);
        User user = userRepository.findById(objectId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        codeRepository.deleteAllByUserId(objectId);
        log.info("Códigos previos eliminados para el usuario: {}", userId);

        generateAndSendCode(user, type);
    }

    /**
     * Inicia el proceso de recuperación de contraseña generando y enviando un código
     * de verificación al correo del usuario.
     *
     * @param email el correo del usuario.
     * @throws UserNotFoundException si no se encuentra el usuario con ese correo.
     */
    @Override
    public void sendPasswordResetCode(String email) {
        log.info("Solicitando código de recuperación de contraseña para: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        codeRepository.deleteAllByUserId(user.getId());
        log.info("Códigos anteriores eliminados para usuario: {}", user.getEmail());

        generateAndSendCode(user, VerificationCodeType.PASSWORD_RESET);
    }

    /**
     * Restablece la contraseña de un usuario si el código de recuperación es válido.
     *
     * @param code el código recibido por correo.
     * @param newPassword la nueva contraseña del usuario.
     * @throws InvalidCodeException si el código no es de tipo PASSWORD_RESET.
     * @throws UserNotFoundException si no se encuentra el usuario asociado.
     */
    @Transactional
    @Override
    public void resetPasswordWithCode(String code, String newPassword) {
        log.info("Procesando restablecimiento de contraseña con código");

        VerificationCode verificationCode = validateCode(code);
        if (verificationCode.getVerificationCodeType() != VerificationCodeType.PASSWORD_RESET) {
            log.warn("Código no válido para recuperación de contraseña: {}", code);
            throw new InvalidCodeException("El código no es válido para recuperar contraseña");
        }

        User user = userRepository.findById(verificationCode.getUserId())
                .orElseThrow(() -> new UserNotFoundException(verificationCode.getUserId().toString()));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        codeRepository.delete(verificationCode);

        log.info("Contraseña restablecida y código eliminado para el usuario: {}", user.getEmail());
    }

    /**
     * Genera un código aleatorio de 6 dígitos.
     *
     * @return código de verificación generado.
     */
    private String generateRandomCode() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(999999));
    }

    /**
     * Valida que el código exista y no haya expirado.
     *
     * @param code el código a validar.
     * @return la entidad de código validado.
     * @throws InvalidCodeException si no se encuentra el código.
     * @throws CodeExpiredException si ha expirado.
     */
    private VerificationCode validateCode(String code) {
        log.info("Validando código de verificación: {}", code);

        VerificationCode verificationCode = codeRepository.findByCode(code)
                .orElseThrow(() -> {
                    log.warn("Código inválido: {}", code);
                    return new InvalidCodeException("Código inválido");
                });

        if (LocalDateTime.now().isAfter(verificationCode.getExpiresAt())) {
            log.warn("Código expirado: {}", code);
            codeRepository.delete(verificationCode);
            throw new CodeExpiredException("El código ha expirado");
        }

        log.debug("Código válido: {} con expiración: {}", code, verificationCode.getExpiresAt());
        return verificationCode;
    }

    /**
     * Activa la cuenta del usuario y elimina el código asociado.
     *
     * @param verificationCode el código validado.
     * @throws UserNotFoundException si no se encuentra el usuario.
     */
    private void validateUserAccount(VerificationCode verificationCode) {
        User user = userRepository.findById(verificationCode.getUserId())
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con ID: {}", verificationCode.getUserId());
                    return new UserNotFoundException(verificationCode.getUserId().toString());
                });

        user.setAccountStatus(AccountStatus.ACTIVATED);
        userRepository.save(user);
        codeRepository.delete(verificationCode);

        log.info("Cuenta activada y código eliminado para usuario: {}", user.getEmail());
    }
}


