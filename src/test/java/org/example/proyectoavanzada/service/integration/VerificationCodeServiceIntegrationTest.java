package org.example.proyectoavanzada.service.integration;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.entity.auth.VerificationCode;
import co.edu.uniquindio.proyecto.entity.auth.VerificationCodeType;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exception.auth.CodeExpiredException;
import co.edu.uniquindio.proyecto.exception.auth.InvalidCodeException;
import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.repository.VerificationCodeRepository;
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.service.interfaces.VerificationService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ContextConfiguration(classes = ProyectoApplication.class)
public class VerificationCodeServiceIntegrationTest {

    @Autowired
    private VerificationCodeRepository codeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private VerificationService verificationService;
    @MockitoBean
    private EmailService emailService;
    private List<User> testUsers;
    private List<VerificationCode> testCodes;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        codeRepository.deleteAll();

        testUsers = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> {
                    User u = new User();
                    u.setEmail("user" + i + "@example.com");
                    u.setPassword(passwordEncoder.encode("password" + i));
                    u.setAccountStatus(AccountStatus.REGISTERED);
                    u.setRol(Rol.USER);
                    return userRepository.save(u);
                })
                .toList();

        testCodes = testUsers.stream()
                .map(user -> {
                    VerificationCode code = new VerificationCode();
                    code.setId(new ObjectId());
                    code.setCode(UUID.randomUUID().toString());
                    code.setUserId(user.getId());
                    code.setCreatedAt(LocalDateTime.now());
                    code.setExpiresAt(LocalDateTime.now().plusMinutes(15));
                    code.setVerificationCodeType(VerificationCodeType.ACTIVATION);
                    return codeRepository.save(code);
                })
                .toList();
    }


    // ------------------------------------------- VALIDATE_ACCOUNT -------------------------------------------- //


    @Test
    @DisplayName("validateCodeActivation - activa cuenta exitosamente con código válido")
    void validateCodeActivation_ValidCode_ActivatesAccount() {
        String code = testCodes.get(0).getCode();
        User user = userRepository.findById(testCodes.get(0).getUserId()).orElseThrow();

        verificationService.validateCodeActivation(code);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVATED, updated.getAccountStatus());
    }

    @Test
    @DisplayName("validateCodeActivation - lanza excepción si el código no existe")
    void validateCodeActivation_InvalidCode_ThrowsException() {
        assertThrows(InvalidCodeException.class,
                () -> verificationService.validateCodeActivation("codigo-inexistente"));
    }

    @Test
    @DisplayName("validateCodeActivation - lanza excepción si el código está expirado")
    void validateCodeActivation_ExpiredCode_ThrowsException() {
        VerificationCode expired = testCodes.get(1);
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        codeRepository.save(expired);

        assertThrows(CodeExpiredException.class,
                () -> verificationService.validateCodeActivation(expired.getCode()));
    }


    // ------------------------------------------- GENERATED_AND_SEND_CODE -------------------------------------------- //

    @Test
    @DisplayName("generateAndSendCode - genera y envía código de activación")
    void generateAndSendCode_SendsActivationEmail() {
        User user = testUsers.get(0);

        verificationService.generateAndSendCode(user, VerificationCodeType.ACTIVATION);

        List<VerificationCode> codes = codeRepository.findAll();
        assertTrue(codes.stream().anyMatch(c ->
                c.getUserId().equals(user.getId()) &&
                        c.getVerificationCodeType() == VerificationCodeType.ACTIVATION
        ));

        verify(emailService, times(1))
                .sendVerificationEmail(eq(user.getEmail()), anyString());
    }

    @Test
    @DisplayName("generateAndSendCode - genera y envía código de restablecimiento")
    void generateAndSendCode_SendsResetEmail() {
        User user = testUsers.get(1);

        verificationService.generateAndSendCode(user, VerificationCodeType.PASSWORD_RESET);

        verify(emailService, times(1))
                .sendPasswordResetEmail(eq(user.getEmail()), anyString());
    }


    // ------------------------------------------- RESEND_CODE -------------------------------------------- //


    @Test
    @DisplayName("Reenvío de código: con userId válido elimina códigos anteriores y genera uno nuevo")
    void givenValidUserId_whenResendCode_thenOldCodesDeletedAndNewCodeGenerated() {
        // Arrange
        User user = testUsers.get(0);
        ObjectId userId = user.getId();
        long initialCount = codeRepository.countByUserId(userId);
        assertTrue(initialCount > 0, "Debe haber códigos iniciales para el usuario");

        // Act
        verificationService.resendCode(userId.toHexString(), VerificationCodeType.ACTIVATION);

        // Assert
        List<VerificationCode> after = codeRepository.findAllByUserId(userId);
        assertEquals(1, after.size(), "Después del reenvío debe quedar un único código");
        VerificationCode newCode = after.get(0);
        assertEquals(VerificationCodeType.ACTIVATION, newCode.getVerificationCodeType(),
                "El tipo de código debe ser ACTIVATION");
        assertNotEquals(testCodes.get(0).getCode(), newCode.getCode(),
                "El código generado debe ser distinto al anterior");
    }


    @Test
    @DisplayName("Reenvío de código: con userId inválido lanza UserNotFoundException")
    void givenInvalidUserId_whenResendCode_thenThrowsUserNotFoundException() {
        // Arrange
        String fakeId = new ObjectId().toHexString();

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                        verificationService.resendCode(fakeId, VerificationCodeType.ACTIVATION),
                "Debe lanzarse UserNotFoundException si el usuario no existe"
        );
    }


    // ------------------------------------------- RESET_PASSWORD -------------------------------------------- //


    @Test
    @DisplayName("Restablecer contraseña: con correo válido elimina códigos anteriores y genera uno nuevo")
    void givenValidEmail_whenSendPasswordResetCode_thenOldCodesDeletedAndNewCodeGenerated() {
        // Arrange
        User user = testUsers.get(1);
        ObjectId userId = user.getId();
        String email = user.getEmail();

        // Act
        verificationService.sendPasswordResetCode(email);

        // Assert
        List<VerificationCode> after = codeRepository.findAllByUserId(userId);
        assertEquals(1, after.size(), "Después del envío debe quedar un único código");
        VerificationCode newCode = after.get(0);
        assertEquals(VerificationCodeType.PASSWORD_RESET, newCode.getVerificationCodeType(),
                "El tipo de código debe ser PASSWORD_RESET");
        assertNotEquals(testCodes.get(1).getCode(), newCode.getCode(),
                "El nuevo código debe ser distinto al anterior");
    }


    @Test
    @DisplayName("Restablecer contraseña: con correo inválido lanza UserNotFoundException")
    void givenInvalidEmail_whenSendPasswordResetCode_thenThrowsUserNotFoundException() {
        // Arrange
        String fakeEmail = "noexiste@example.com";

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                        verificationService.sendPasswordResetCode(fakeEmail),
                "Debe lanzarse UserNotFoundException si el correo no corresponde a ningún usuario"
        );
    }

    @Test
    @DisplayName("Restablecer contraseña: con código válido cambia contraseña y elimina el código")
    void givenValidPasswordResetCode_whenResetPasswordWithCode_thenPasswordIsChangedAndCodeDeleted() {
        // Arrange
        User user = testUsers.get(0);
        VerificationCode code = testCodes.get(0);
        code.setVerificationCodeType(VerificationCodeType.PASSWORD_RESET);
        codeRepository.save(code);
        String newPassword = "nuevoPassword123";

        // Act
        verificationService.resetPasswordWithCode(code.getCode(), newPassword);

        // Assert
        // Verificar que la contraseña fue actualizada
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches(newPassword, updatedUser.getPassword()),
                "La nueva contraseña debe coincidir con la codificada");

        // Verificar que el código fue eliminado
        assertFalse(codeRepository.findById(code.getId().toHexString()).isPresent(),
                "El código debe haberse eliminado después del restablecimiento");
    }

    @Test
    @DisplayName("Restablecer contraseña: falla si el código no es de tipo PASSWORD_RESET")
    void givenActivationCode_whenResetPasswordWithCode_thenThrowsInvalidCodeException() {
        // Arrange
        VerificationCode code = testCodes.get(1); // ya es ACTIVATION

        // Act & Assert
        InvalidCodeException ex = assertThrows(InvalidCodeException.class, () ->
                verificationService.resetPasswordWithCode(code.getCode(), "otroPass123"));

        assertEquals("El código no es válido para recuperar contraseña", ex.getMessage());
    }

    @Test
    @DisplayName("Restablecer contraseña: falla si el usuario del código no existe")
    void givenCodeWithInvalidUserId_whenResetPasswordWithCode_thenThrowsUserNotFoundException() {
        // Arrange
        VerificationCode code = testCodes.get(2);
        code.setVerificationCodeType(VerificationCodeType.PASSWORD_RESET);
        code.setUserId(new ObjectId()); // usuario inexistente
        codeRepository.save(code);

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                verificationService.resetPasswordWithCode(code.getCode(), "fakeUserPass123"));
    }

}
