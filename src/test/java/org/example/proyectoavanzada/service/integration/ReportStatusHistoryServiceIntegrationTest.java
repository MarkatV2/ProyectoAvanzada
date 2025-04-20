package org.example.proyectoavanzada.service.integration;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.dto.report.PaginatedHistoryResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusHistoryResponse;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.entity.report.ReportStatusHistory;
import co.edu.uniquindio.proyecto.entity.user.User;
import co.edu.uniquindio.proyecto.exception.report.HistoryNotFoundException;
import co.edu.uniquindio.proyecto.exception.report.ReportNotFoundException;
import co.edu.uniquindio.proyecto.exception.user.UserNotFoundException;
import co.edu.uniquindio.proyecto.repository.ReportStatusHistoryRepository;
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.service.interfaces.ReportStatusHistoryService;
import co.edu.uniquindio.proyecto.service.mapper.ReportStatusHistoryMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(classes = ProyectoApplication.class)
public class ReportStatusHistoryServiceIntegrationTest {

    @Autowired
    private ReportStatusHistoryService reportStatusHistoryService;

    @Autowired
    private ReportStatusHistoryRepository historyRepository;

    @Autowired
    private ReportStatusHistoryMapper historyMapper;
    @Autowired
    private MongoTemplate mongoTemplate;
    @MockitoBean
    private EmailService emailService;

    private List<ReportStatusHistory> testHistories;
    private ObjectId sharedReportId;
    private ObjectId sharedUserId;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();

        sharedReportId = new ObjectId();
        sharedUserId = new ObjectId();

        Report r = new Report();
        r.setId(sharedReportId);

        User user = new User();
        user.setId(sharedUserId);

        // Creamos 5 historiales con el mismo reportId y userId
        testHistories = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> {
                    ReportStatusHistory history = new ReportStatusHistory();
                    history.setId(new ObjectId());
                    history.setReportId(sharedReportId);
                    history.setUserId(sharedUserId);
                    history.setPreviousStatus(ReportStatus.PENDING);
                    history.setNewStatus(ReportStatus.values()[(i % ReportStatus.values().length)]);
                    history.setChangedAt(LocalDateTime.now().minusDays(i));
                    return historyRepository.save(history);
                })
                .toList();

        mongoTemplate.insert(user);
        mongoTemplate.insert(r);
    }


    // ------------------------------------------- CREATE_HISTORY -------------------------------------------- //


    @Test
    @DisplayName("Crear historial: crea y guarda un historial correctamente en la base de datos")
    void givenValidData_whenCreateHistory_thenHistoryIsSaved() {
        // Arrange
        ObjectId reportId = new ObjectId();
        ObjectId userId = new ObjectId();
        ReportStatus previous = ReportStatus.VERIFIED;
        ReportStatus next = ReportStatus.RESOLVED;

        // Act
        reportStatusHistoryService.createHistory(reportId, userId, previous, next);

        // Assert
        List<ReportStatusHistory> histories = historyRepository.findAll();
        assertEquals(6, histories.size(), "Debe haber 6 historiales luego de crear uno nuevo");

        ReportStatusHistory created = histories.get(histories.size() - 1);
        assertEquals(reportId, created.getReportId());
        assertEquals(userId, created.getUserId());
        assertEquals(previous, created.getPreviousStatus());
        assertEquals(next, created.getNewStatus());
        assertNotNull(created.getChangedAt(), "La fecha de cambio debe haberse generado");
    }


    // ------------------------------------------- GET_HISTORY_BY_ID -------------------------------------------- //


    @Test
    @DisplayName("Obtener historial por ID: retorna el historial correspondiente si existe")
    void givenValidHistoryId_whenGetHistoryById_thenReturnsCorrectResponse() {
        // Arrange
        ReportStatusHistory history = testHistories.get(0);
        String historyId = history.getId().toHexString();

        // Act
        ReportStatusHistoryResponse response = reportStatusHistoryService.getHistoryById(historyId);

        // Assert
        assertEquals(historyId, response.id());
        assertEquals(history.getReportId().toHexString(), response.reportId());
        assertEquals(history.getUserId().toHexString(), response.userId());
        assertEquals(history.getPreviousStatus(), response.previousStatus());
        assertEquals(history.getNewStatus(), response.newStatus());
    }

    @Test
    @DisplayName("Obtener historial por ID: lanza excepción si el historial no existe")
    void givenInvalidHistoryId_whenGetHistoryById_thenThrowsException() {
        // Arrange
        String nonExistentId = new ObjectId().toHexString();

        // Act & Assert
        HistoryNotFoundException ex = assertThrows(HistoryNotFoundException.class, () ->
                reportStatusHistoryService.getHistoryById(nonExistentId));

        assertEquals("Historial no encontrado con ID: " + nonExistentId, ex.getMessage());
    }


    // ------------------------------------------- GET_HISTORY_BY_REPORT_ID -------------------------------------------- //


    @Test
    @DisplayName("Buscar historial por ID de reporte inexistente debe lanzar ReportNotFoundException")
    void givenInvalidReportId_whenGetHistoryByReportId_thenThrowReportNotFoundException() {
        String invalidReportId = new ObjectId().toHexString();

        ReportNotFoundException exception = assertThrows(ReportNotFoundException.class, () -> {
            reportStatusHistoryService.getHistoryByReportId(invalidReportId, 1, 5);
        });

        assertEquals("Reporte no encontrado con ID: " + invalidReportId, exception.getMessage());
    }

    @Test
    @DisplayName("Buscar por ID de reporte: retorna historial paginado correctamente")
    void givenReportId_whenGetHistoryByReportId_thenReturnsPaginatedHistory() {
        // Act
        PaginatedHistoryResponse response = reportStatusHistoryService
                .getHistoryByReportId(sharedReportId.toHexString(), 1, 5);


        assertEquals(1, response.page());
        assertEquals(5, response.size());
        assertEquals(5, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals(5, response.content().size());
    }

    @Test
    @DisplayName("Buscar historial por ID de reporte devuelve correctamente la segunda página")
    void givenMultipleHistories_whenGetHistoryByReportIdPage2_thenReturnsCorrectPagination() {
        String reportId = testHistories.get(0).getReportId().toHexString();

        PaginatedHistoryResponse response = reportStatusHistoryService.getHistoryByReportId(reportId, 2, 2);

        assertEquals(2, response.page());
        assertEquals(2, response.size());
        assertEquals(5, response.totalElements());
        assertEquals(3, response.totalPages());
        assertEquals(2, response.content().size());
    }


    // ------------------------------------------- GET_HISTORY_BY_USER_ID -------------------------------------------- //


    @Test
    @DisplayName("Buscar por ID de usuario: retorna historial paginado correctamente")
    void givenUserId_whenGetHistoryByUserId_thenReturnsPaginatedHistory() {
        // Act
        PaginatedHistoryResponse response = reportStatusHistoryService
                .getHistoryByUserId(sharedUserId.toHexString(), 1, 5);

        // Assert
        assertEquals(1, response.page());
        assertEquals(5, response.size());
        assertEquals(5, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals(5, response.content().size());
    }


    @Test
    @DisplayName("Buscar historial por ID de usuario inexistente debe lanzar UserNotFoundException")
    void givenInvalidUserId_whenGetHistoryByUserId_thenThrowUserNotFoundException() {
        String invalidUserId = new ObjectId().toHexString();

        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            reportStatusHistoryService.getHistoryByUserId(invalidUserId, 1, 5);
        });

        assertEquals("Usuario no encontrado: " + invalidUserId, exception.getMessage());
    }


    // ------------------------------------------- GET_HISTORY_BY_OLD_STATUS -------------------------------------------- //


    @Test
    @DisplayName("Buscar historial por estado anterior devuelve resultados correctamente")
    void givenPreviousStatus_whenGetHistoryByPreviousStatus_thenReturnFilteredResults() {
        ReportStatus previousStatus = testHistories.get(0).getPreviousStatus();
        String reportId = testHistories.get(0).getReportId().toHexString();

        PaginatedHistoryResponse response = reportStatusHistoryService.getHistoryByPreviousStatus(reportId, previousStatus, 1, 5);

        assertFalse(response.content().isEmpty());
        assertTrue(response.content().stream().allMatch(h -> h.previousStatus() == previousStatus));
        assertEquals(1, response.page());
    }

    @Test
    @DisplayName("Buscar historial por estado anterior inexistente devuelve lista vacía")
    void givenNonExistentPreviousStatus_whenGetHistoryByPreviousStatus_thenReturnEmptyList() {
        String reportId = testHistories.get(0).getReportId().toHexString();

        PaginatedHistoryResponse response = reportStatusHistoryService.getHistoryByPreviousStatus(
                reportId, ReportStatus.DELETED, 1, 5);

        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
    }


    // ------------------------------------------- GET_HISTORY_BY_NEW_STATUS_AND_DATE_RANGE -------------------------------------------- //


    @Test
    @DisplayName("Buscar historial por nuevo estado y rango de fechas devuelve resultados")
    void givenValidDateRangeAndStatus_whenGetHistoryByNewStatusAndDateRange_thenReturnResults() {
        String reportId = testHistories.get(0).getReportId().toHexString();
        ReportStatus newStatus = testHistories.get(0).getNewStatus();
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(2);

        PaginatedHistoryResponse response = reportStatusHistoryService.getHistoryByNewStatusAndDateRange(
                reportId, newStatus, start, end, 1, 5);

        assertFalse(response.content().isEmpty());
        assertTrue(response.content().stream().allMatch(h -> h.newStatus() == newStatus));
    }

    @Test
    @DisplayName("Buscar historial fuera del rango de fechas devuelve lista vacía")
    void givenOutOfRangeDates_whenGetHistoryByNewStatusAndDateRange_thenReturnEmpty() {
        String reportId = testHistories.get(0).getReportId().toHexString();
        ReportStatus newStatus = testHistories.get(0).getNewStatus();
        LocalDateTime start = LocalDateTime.now().minusYears(5);
        LocalDateTime end = LocalDateTime.now().minusYears(4);

        PaginatedHistoryResponse response = reportStatusHistoryService.getHistoryByNewStatusAndDateRange(
                reportId, newStatus, start, end, 1, 5);

        assertTrue(response.content().isEmpty());
    }


    // ------------------------------------------- COUNT_BY_REPORT -------------------------------------------- //


    @Test
    @DisplayName("Contar cambios de estado por ID de reporte devuelve cantidad correcta")
    void givenReportId_whenCountHistoryByReportId_thenReturnCorrectCount() {
        String reportId = testHistories.get(0).getReportId().toHexString();

        long count = reportStatusHistoryService.countHistoryByReportId(reportId);

        long expected = testHistories.stream()
                .filter(h -> h.getReportId().toHexString().equals(reportId))
                .count();

        assertEquals(expected, count);
    }

}
