package org.example.proyectoavanzada.repository;


import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.entity.auth.VerificationCode;
import co.edu.uniquindio.proyecto.entity.auth.VerificationCodeType;
import co.edu.uniquindio.proyecto.repository.VerificationCodeRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;

@DataMongoTest
@ContextConfiguration(classes = ProyectoApplication.class)
class VerificationCodeRepositoryTest {

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    // Definir dos usuarios para asociar los códigos
    private final ObjectId userId1 = new ObjectId();
    private final ObjectId userId2 = new ObjectId();

    @BeforeEach
    void setUp() {
        // Limpiar la colección para garantizar pruebas repetibles
        verificationCodeRepository.deleteAll();

        // Lista para almacenar los códigos de verificación de prueba
        List<VerificationCode> codes = new ArrayList<>();

        // Código 1: Usuario 1, tipo ACTIVATION
        VerificationCode vc1 = new VerificationCode();
        vc1.setId(new ObjectId());
        vc1.setCode("CODE1");
        vc1.setUserId(userId1);
        vc1.setCreatedAt(LocalDateTime.now());
        vc1.setExpiresAt(LocalDateTime.now().plusHours(1));
        vc1.setVerificationCodeType(VerificationCodeType.ACTIVATION);
        codes.add(vc1);

        // Código 2: Usuario 1, tipo PASSWORD_RESET
        VerificationCode vc2 = new VerificationCode();
        vc2.setId(new ObjectId());
        vc2.setCode("CODE2");
        vc2.setUserId(userId1);
        vc2.setCreatedAt(LocalDateTime.now());
        vc2.setExpiresAt(LocalDateTime.now().plusHours(1));
        vc2.setVerificationCodeType(VerificationCodeType.PASSWORD_RESET);
        codes.add(vc2);

        // Código 3: Usuario 1, tipo ACTIVATION
        VerificationCode vc3 = new VerificationCode();
        vc3.setId(new ObjectId());
        vc3.setCode("CODE3");
        vc3.setUserId(userId1);
        vc3.setCreatedAt(LocalDateTime.now());
        vc3.setExpiresAt(LocalDateTime.now().plusHours(1));
        vc3.setVerificationCodeType(VerificationCodeType.ACTIVATION);
        codes.add(vc3);

        // Código 4: Usuario 2, tipo PASSWORD_RESET
        VerificationCode vc4 = new VerificationCode();
        vc4.setId(new ObjectId());
        vc4.setCode("CODE4");
        vc4.setUserId(userId2);
        vc4.setCreatedAt(LocalDateTime.now());
        vc4.setExpiresAt(LocalDateTime.now().plusHours(1));
        vc4.setVerificationCodeType(VerificationCodeType.PASSWORD_RESET);
        codes.add(vc4);

        // Código 5: Usuario 2, tipo ACTIVATION
        VerificationCode vc5 = new VerificationCode();
        vc5.setId(new ObjectId());
        vc5.setCode("CODE5");
        vc5.setUserId(userId2);
        vc5.setCreatedAt(LocalDateTime.now());
        vc5.setExpiresAt(LocalDateTime.now().plusHours(1));
        vc5.setVerificationCodeType(VerificationCodeType.ACTIVATION);
        codes.add(vc5);

        // Persistir los datos de prueba en la base de datos
        verificationCodeRepository.saveAll(codes);
    }

    @Test
    @DisplayName("Buscar código de verificación existente")
    void testFindByCodeExists() {
        // Act: Buscar el código "CODE1"
        Optional<VerificationCode> result = verificationCodeRepository.findByCode("CODE1");
        // Assert: Se debe encontrar y el código debe ser "CODE1"
        assertTrue(result.isPresent(), "El código CODE1 debe existir");
        assertEquals("CODE1", result.get().getCode());
    }

    @Test
    @DisplayName("Buscar código de verificación inexistente")
    void testFindByCodeNotFound() {
        // Act: Buscar un código que no existe
        Optional<VerificationCode> result = verificationCodeRepository.findByCode("INVALID_CODE");
        // Assert: Se debe retornar un Optional vacío
        assertFalse(result.isPresent(), "No se debe encontrar código para 'INVALID_CODE'");
    }

    @Test
    @DisplayName("Eliminar códigos de verificación por userId (usuario 1)")
    void testDeleteAllByUserId_User1() {
        // Act: Eliminar los códigos asociados a userId1
        verificationCodeRepository.deleteAllByUserId(userId1);
        // Assert: Solo deben quedar los códigos del usuario 2 (2 códigos)
        List<VerificationCode> remainingCodes = verificationCodeRepository.findAll();
        assertEquals(2, remainingCodes.size(), "Solo deben quedar 2 códigos para el usuario 2");
        remainingCodes.forEach(code -> assertEquals(userId2, code.getUserId()));
    }

    @Test
    @DisplayName("Eliminar códigos de verificación por userId (usuario 2)")
    void testDeleteAllByUserId_User2() {
        // Act: Eliminar los códigos asociados a userId2
        verificationCodeRepository.deleteAllByUserId(userId2);
        // Assert: Deben quedar 3 códigos asociados al usuario 1
        List<VerificationCode> remainingCodes = verificationCodeRepository.findAll();
        assertEquals(3, remainingCodes.size(), "Deben quedar 3 códigos para el usuario 1");
        remainingCodes.forEach(code -> assertEquals(userId1, code.getUserId()));
    }

    @Test
    @DisplayName("Verificar que se cargaron 5 códigos de verificación")
    void testCountCodes() {
        // Act: Contar la cantidad de códigos en la base de datos
        long count = verificationCodeRepository.count();
        // Assert: Se deben haber persistido 5 códigos
        assertEquals(5, count, "Se deben cargar 5 códigos de verificación");
    }
}

