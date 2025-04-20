package org.example.proyectoavanzada.service.unit;

import co.edu.uniquindio.proyecto.entity.auth.VerificationCode;
import co.edu.uniquindio.proyecto.entity.auth.VerificationCodeType;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exception.auth.CodeExpiredException;
import co.edu.uniquindio.proyecto.exception.auth.InvalidCodeException;
import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.repository.VerificationCodeRepository;
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.service.implementations.VerificationServiceImpl;
import co.edu.uniquindio.proyecto.service.mapper.VerificationCodeMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceUnitTest {
    private static final int EXPIRATION_MINUTES = 15;

    @Mock
    private VerificationCodeRepository codeRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VerificationCodeMapper verificationCodeMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private VerificationServiceImpl verificationService;

    private User testUser;
    private List<VerificationCode> codes;

    @BeforeEach
    void setUp() {
        // Arrange: Prepare a user and a set of sample codes
        testUser = new User();
        testUser.setId(new ObjectId());
        testUser.setEmail("test@example.com");

        codes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            VerificationCode code = new VerificationCode();
            code.setCode("CODE" + i);
            code.setUserId(testUser.getId());
            code.setCreatedAt(LocalDateTime.now().minusMinutes(i));
            code.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES - i));
            code.setVerificationCodeType(VerificationCodeType.ACTIVATION);
            codes.add(code);
        }
    }


    // ------------------------------------------- GENERATE_AND_SEND_CODE -------------------------------------------- //


    @Test
    @DisplayName("generateAndSendCode should save code and send activation email when type is ACTIVATION")
    void testGenerateAndSendCode_Activation() {
        // Arrange
        VerificationCode expectedCode = codes.get(0);
        when(verificationCodeMapper.toVerificationCode(anyString(), eq(testUser), eq(EXPIRATION_MINUTES)))
                .thenReturn(expectedCode);

        // Act
        verificationService.generateAndSendCode(testUser, VerificationCodeType.ACTIVATION);

        // Assert
        verify(codeRepository, times(1)).save(expectedCode);
    }

    @Test
    @DisplayName("generateAndSendCode should save code and send password reset email when type is PASSWORD_RESET")
    void testGenerateAndSendCode_PasswordReset() {
        // Arrange
        VerificationCode expectedCode = codes.get(1);
        when(verificationCodeMapper.toVerificationCode(anyString(), eq(testUser), eq(EXPIRATION_MINUTES)))
                .thenReturn(expectedCode);

        // Act
        verificationService.generateAndSendCode(testUser, VerificationCodeType.PASSWORD_RESET);

        // Assert
        verify(codeRepository, times(1)).save(expectedCode);
    }


    // ------------------------------------------- VALIDATE_CODE -------------------------------------------- //


    @Test
    @DisplayName("validateCodeActivation should activate account and delete code when code is valid and not expired")
    void testValidateCodeActivation_Success() {
        // Arrange
        VerificationCode validCode = codes.get(2);
        when(codeRepository.findByCode("CODE2")).thenReturn(Optional.of(validCode));
        when(userRepository.findById(validCode.getUserId())).thenReturn(Optional.of(testUser));

        // Act & Assert: no debe lanzar excepción
        assertDoesNotThrow(() -> verificationService.validateCodeActivation("CODE2"));

        // Verify behavior: cuenta activada y código eliminado
        assertEquals(AccountStatus.ACTIVATED, testUser.getAccountStatus());
        verify(userRepository, times(1)).save(testUser);
        verify(codeRepository, times(1)).delete(validCode);
    }

    @Test
    @DisplayName("validateCodeActivation should throw InvalidCodeException when code does not exist")
    void testValidateCodeActivation_InvalidCode() {
        // Arrange
        when(codeRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCodeException.class,
                () -> verificationService.validateCodeActivation("INVALID"));
    }

    @Test
    @DisplayName("validateCodeActivation should throw CodeExpiredException and delete code when expired")
    void testValidateCodeActivation_ExpiredCode() {
        // Arrange
        VerificationCode expiredCode = new VerificationCode();
        expiredCode.setCode("EXPIRED");
        expiredCode.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(codeRepository.findByCode("EXPIRED")).thenReturn(Optional.of(expiredCode));

        // Act & Assert
        assertThrows(CodeExpiredException.class,
                () -> verificationService.validateCodeActivation("EXPIRED"));
        verify(codeRepository, times(1)).delete(expiredCode);
    }


    // ------------------------------------------- RESEND_CODE -------------------------------------------- //

    @Test
    @DisplayName("resendCode debería eliminar códigos previos y reenviar un nuevo código de ACTIVATION cuando el usuario existe")
    void testResendCode_Success() {

        // Arrange
        VerificationCode expectedCode = codes.get(0);
        when(verificationCodeMapper.toVerificationCode(anyString(), eq(testUser), eq(EXPIRATION_MINUTES)))
                .thenReturn(expectedCode);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        // Act
        verificationService.resendCode(testUser.getId().toHexString(), VerificationCodeType.ACTIVATION);

        // Assert
        verify(codeRepository, times(1)).deleteAllByUserId(eq(testUser.getId()));
        verify(codeRepository, times(1)).save(expectedCode);
    }

    @Test
    @DisplayName("resendCode debería lanzar UserNotFoundException cuando el usuario no existe")
    void testResendCode_UserNotFound() {
        // Arrange
        when(userRepository.findById(any(ObjectId.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class,
                () -> verificationService.resendCode(testUser.getId().toHexString(), VerificationCodeType.ACTIVATION));
    }


    // ------------------------------------------- PASSWORD_RESET_CODE -------------------------------------------- //


    @Test
    @DisplayName("sendPasswordResetCode debería eliminar códigos previos y enviar código de PASSWORD_RESET cuando el email existe")
    void testSendPasswordResetCode_Success() {
        // Arrange
        VerificationCode expectedCode = codes.get(0);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(verificationCodeMapper.toVerificationCode(anyString(), eq(testUser), eq(EXPIRATION_MINUTES)))
                .thenReturn(expectedCode);

        // Act
        verificationService.sendPasswordResetCode(testUser.getEmail());

        // Assert
        verify(codeRepository, times(1)).deleteAllByUserId(eq(testUser.getId()));
        verify(codeRepository, times(1)).save(expectedCode);
    }

    @Test
    @DisplayName("sendPasswordResetCode debería lanzar UserNotFoundException cuando el email no está registrado")
    void testSendPasswordResetCode_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class,
                () -> verificationService.sendPasswordResetCode("nonexistent@example.com"));
    }


    // ------------------------------------------- RESET_PASSWORD_WITH_CODE-------------------------------------------- //


    @Test
    @DisplayName("resetPasswordWithCode: éxito cuando el código es PASSWORD_RESET y usuario existe")
    void testResetPasswordWithCode_Success() {
        // Arrange
        VerificationCode vcode = new VerificationCode();
        vcode.setCode("RESET");
        vcode.setUserId(testUser.getId());
        vcode.setVerificationCodeType(VerificationCodeType.PASSWORD_RESET);
        vcode.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        vcode.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(codeRepository.findByCode("RESET")).thenReturn(Optional.of(vcode));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPass")).thenReturn("ENCODED");

        // Act
        verificationService.resetPasswordWithCode("RESET", "newPass");

        // Assert
        assertEquals("ENCODED", testUser.getPassword());
        verify(userRepository).save(testUser);
        verify(codeRepository).delete(vcode);
    }

    @Test
    @DisplayName("resetPasswordWithCode: lanza InvalidCodeException si el código no existe")
    void testResetPasswordWithCode_CodeNotFound() {
        when(codeRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        assertThrows(InvalidCodeException.class,
                () -> verificationService.resetPasswordWithCode("INVALID", "pass"));
    }

    @Test
    @DisplayName("resetPasswordWithCode: lanza InvalidCodeException si el tipo no es PASSWORD_RESET")
    void testResetPasswordWithCode_WrongType() {
        VerificationCode wrongTypeCode = codes.get(0); // ACTIVATION
        wrongTypeCode.setCode("CODE0");

        when(codeRepository.findByCode("CODE0")).thenReturn(Optional.of(wrongTypeCode));

        assertThrows(InvalidCodeException.class,
                () -> verificationService.resetPasswordWithCode("CODE0", "pass"));
    }

    @Test
    @DisplayName("resetPasswordWithCode: lanza UserNotFoundException si el usuario no existe")
    void testResetPasswordWithCode_UserNotFound() {
        VerificationCode vcode = new VerificationCode();
        vcode.setCode("RESET");
        vcode.setUserId(testUser.getId());
        vcode.setVerificationCodeType(VerificationCodeType.PASSWORD_RESET);
        vcode.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(codeRepository.findByCode("RESET")).thenReturn(Optional.of(vcode));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> verificationService.resetPasswordWithCode("RESET", "pass"));
    }

}
