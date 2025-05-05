package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.entity.auth.VerificationCodeType;
import co.edu.uniquindio.proyecto.entity.user.User;

/**
 * Servicio para manejar códigos de verificación y recuperación de contraseñas.
 */
public interface VerificationService {

    /**
     * Genera y envía un código de verificación al usuario.
     *
     * @param user usuario al que se le enviará el código.
     * @param type tipo de código (activación o recuperación).
     */
    void generateAndSendCode(User user, VerificationCodeType type);

    /**
     * Valida un código de activación de cuenta.
     *
     * @param code código a validar.
     */
    void validateCodeActivation(String code);

    /**
     * Reenvía un código de verificación al usuario.
     *
     * @param email ID del usuario.
     * @param type   tipo de código.
     */
    void resendCode(String email, VerificationCodeType type);

    /**
     * Envía un código de recuperación de contraseña al correo.
     *
     * @param email correo del usuario.
     */
    void sendPasswordResetCode(String email);

    /**
     * Restablece la contraseña con un código válido.
     *
     * @param code        código de recuperación.
     * @param newPassword nueva contraseña.
     */
    void resetPasswordWithCode(String code, String newPassword);
}

