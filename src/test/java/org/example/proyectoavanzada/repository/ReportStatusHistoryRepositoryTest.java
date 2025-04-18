package org.example.proyectoavanzada.repository;



import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.entity.report.ReportStatusHistory;
import co.edu.uniquindio.proyecto.repository.ReportStatusHistoryRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;

@DataMongoTest
@ContextConfiguration(classes = ProyectoApplication.class)
class ReportStatusHistoryRepositoryTest {

    @Autowired
    private ReportStatusHistoryRepository historyRepository;

    // Definición de ObjectId para reportes y usuarios de prueba
    private final ObjectId reportId1 = new ObjectId(); // Reporte para el que se crean varias historias
    private final ObjectId reportId2 = new ObjectId(); // Otro reporte para probar el conteo
    private final ObjectId userId1 = new ObjectId();
    private final ObjectId userId2 = new ObjectId();

    @BeforeEach
    void setUp() {
        // Se elimina la colección para garantizar que cada prueba se ejecute en un entorno limpio
        historyRepository.deleteAll();

        // Lista para almacenar los historiales de prueba
        List<ReportStatusHistory> histories = new ArrayList<>();

        // Historial 1: Para reportId1, usuario userId1, de PENDING -> VERIFIED, hace 3 días
        ReportStatusHistory h1 = new ReportStatusHistory();
        h1.setId(new ObjectId());
        h1.setReportId(reportId1);
        h1.setUserId(userId1);
        h1.setPreviousStatus(ReportStatus.PENDING);
        h1.setNewStatus(ReportStatus.VERIFIED);
        h1.setChangedAt(LocalDateTime.now().minusDays(3));
        histories.add(h1);

        // Historial 2: Para reportId1, usuario userId1, de VERIFIED -> RESOLVED, hace 2 días
        ReportStatusHistory h2 = new ReportStatusHistory();
        h2.setId(new ObjectId());
        h2.setReportId(reportId1);
        h2.setUserId(userId1);
        h2.setPreviousStatus(ReportStatus.VERIFIED);
        h2.setNewStatus(ReportStatus.RESOLVED);
        h2.setChangedAt(LocalDateTime.now().minusDays(2));
        histories.add(h2);

        // Historial 3: Para reportId1, usuario userId2, de RESOLVED -> REJECTED, hace 1 día
        ReportStatusHistory h3 = new ReportStatusHistory();
        h3.setId(new ObjectId());
        h3.setReportId(reportId1);
        h3.setUserId(userId2);
        h3.setPreviousStatus(ReportStatus.RESOLVED);
        h3.setNewStatus(ReportStatus.REJECTED);
        h3.setChangedAt(LocalDateTime.now().minusDays(1));
        histories.add(h3);

        // Historial 4: Para reportId2, usuario userId1, de PENDING -> DELETED, hace 5 días
        ReportStatusHistory h4 = new ReportStatusHistory();
        h4.setId(new ObjectId());
        h4.setReportId(reportId2);
        h4.setUserId(userId1);
        h4.setPreviousStatus(ReportStatus.PENDING);
        h4.setNewStatus(ReportStatus.DELETED);
        h4.setChangedAt(LocalDateTime.now().minusDays(5));
        histories.add(h4);

        // Historial 5: Para reportId1, usuario userId1, de REJECTED -> DELETED, en el momento actual
        ReportStatusHistory h5 = new ReportStatusHistory();
        h5.setId(new ObjectId());
        h5.setReportId(reportId1);
        h5.setUserId(userId1);
        h5.setPreviousStatus(ReportStatus.REJECTED);
        h5.setNewStatus(ReportStatus.DELETED);
        h5.setChangedAt(LocalDateTime.now());
        histories.add(h5);

        // Se guardan todos los historiales en la base de datos
        historyRepository.saveAll(histories);
    }

    @Test
    @DisplayName("Obtener historial por reportId")
    void testFindByReportId() {
        // Arrange: Se usa reportId1, para el cual se han creado 4 historiales (h1, h2, h3 y h5)
        PageRequest pageable = PageRequest.of(0, 10);
        // Act: Se recupera el historial por reportId1
        Page<ReportStatusHistory> result = historyRepository.findByReportId(reportId1, pageable);
        // Assert: Se espera encontrar 4 registros
        assertEquals(4, result.getTotalElements(), "Se deben recuperar 4 historiales para reportId1");
    }

    @Test
    @DisplayName("Obtener historial por reportId en rango de fechas")
    void testFindByReportIdAndDateRange() {
        // Arrange: Definir un rango de fechas para reportId1 en el que solo se incluyen h2, h3 y h5
        LocalDateTime start = LocalDateTime.now().minusDays(2).minusHours(12);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        PageRequest pageable = PageRequest.of(0, 10);
        // Act: Se recupera el historial de reportId1 en el rango de fechas definido
        Page<ReportStatusHistory> result = historyRepository.findByReportIdAndDateRange(reportId1, start, end, pageable);
        // Assert: Se espera obtener 3 registros
        assertEquals(3, result.getTotalElements(), "Se deben recuperar 3 historiales en el rango de fechas para reportId1");
    }

    @Test
    @DisplayName("Obtener historial por reportId y previousStatus")
    void testFindByReportIdAndPreviousStatus() {
        // Arrange: Para reportId1 y previousStatus VERIFIED, solo debería coincidir h2
        PageRequest pageable = PageRequest.of(0, 10);
        // Act: Se obtiene el historial filtrado
        Page<ReportStatusHistory> result = historyRepository.findByReportIdAndPreviousStatus(reportId1, ReportStatus.VERIFIED, pageable);
        // Assert: Se espera un único registro
        assertEquals(1, result.getTotalElements(), "Se debe recuperar 1 historial con previousStatus VERIFIED para reportId1");
    }

    @Test
    @DisplayName("Obtener historial por userId")
    void testFindByUserId() {
        // Arrange: Para userId1 se encuentran h1, h2, h4 y h5 (4 registros en total)
        PageRequest pageable = PageRequest.of(0, 10);
        // Act: Se recupera el historial para userId1
        Page<ReportStatusHistory> result = historyRepository.findByUserId(userId1, pageable);
        // Assert: Se esperan 4 registros
        assertEquals(4, result.getTotalElements(), "Se deben recuperar 4 historiales para userId1");
    }

    @Test
    @DisplayName("Contar cambios de estado por reportId")
    void testCountByReportId() {
        // Act: Para reportId1 se han creado 4 historiales
        long count = historyRepository.countByReportId(reportId1);
        // Assert:
        assertEquals(4, count, "El conteo de historiales para reportId1 debe ser 4");
    }

    @Test
    @DisplayName("Obtener historial por reportId, newStatus y rango de fechas")
    void testFindByReportIdAndNewStatusAndDateRange() {
        // Arrange: Para reportId1, newStatus DELETED y en un rango que incluya el registro h5 (cambios recientes)
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        PageRequest pageable = PageRequest.of(0, 10);
        // Act: Se recupera el historial filtrado
        Page<ReportStatusHistory> result = historyRepository.findByReportIdAndNewStatusAndDateRange(
                reportId1, ReportStatus.DELETED, start, end, pageable);
        // Assert: Solo h5 debe cumplir con estas condiciones
        assertEquals(1, result.getTotalElements(), "Se debe recuperar 1 historial con newStatus DELETED en el rango de fechas para reportId1");
    }
}

