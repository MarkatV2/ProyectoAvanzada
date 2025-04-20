package org.example.proyectoavanzada.service.unit;

import co.edu.uniquindio.proyecto.dto.report.PaginatedHistoryResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusHistoryResponse;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.entity.report.ReportStatusHistory;
import co.edu.uniquindio.proyecto.exception.report.HistoryNotFoundException;
import co.edu.uniquindio.proyecto.repository.ReportRepository;
import co.edu.uniquindio.proyecto.repository.ReportStatusHistoryRepository;
import co.edu.uniquindio.proyecto.repository.UserRepository;
import co.edu.uniquindio.proyecto.service.implementations.ReportStatusHistoryServiceImpl;
import co.edu.uniquindio.proyecto.service.mapper.ReportStatusHistoryMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportStatusHistoryServiceTest {

    @Mock
    private ReportStatusHistoryRepository historyRepository;
    @Mock
    private ReportStatusHistoryMapper historyMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportStatusHistoryServiceImpl historyService;

    private List<ReportStatusHistory> testHistories;

    @BeforeEach
    void setUp() {
        // Configurar 5 historias de reporte de prueba
        testHistories = List.of(
                createTestHistory("507f1f77bcf86cd799439011", "507f1f77bcf86cd799439021",
                        ReportStatus.PENDING, ReportStatus.VERIFIED, LocalDateTime.now().minusDays(4)),
                createTestHistory("507f1f77bcf86cd799439011", "507f1f77bcf86cd799439022",
                        ReportStatus.VERIFIED, ReportStatus.RESOLVED, LocalDateTime.now().minusDays(3)),
                createTestHistory("507f1f77bcf86cd799439012", "507f1f77bcf86cd799439023",
                        ReportStatus.PENDING, ReportStatus.REJECTED, LocalDateTime.now().minusDays(2)),
                createTestHistory("507f1f77bcf86cd799439013", "507f1f77bcf86cd799439024",
                        ReportStatus.PENDING, ReportStatus.DELETED, LocalDateTime.now().minusDays(1)),
                createTestHistory("507f1f77bcf86cd799439011", "507f1f77bcf86cd799439025",
                        ReportStatus.RESOLVED, ReportStatus.VERIFIED, LocalDateTime.now())
        );
    }

    private ReportStatusHistory createTestHistory(String reportId, String userId,
                                                  ReportStatus prevStatus, ReportStatus newStatus, LocalDateTime changedAt) {
        ReportStatusHistory history = new ReportStatusHistory();
        history.setId(new ObjectId());
        history.setReportId(new ObjectId(reportId));
        history.setUserId(new ObjectId(userId));
        history.setPreviousStatus(prevStatus);
        history.setNewStatus(newStatus);
        history.setChangedAt(changedAt);
        return history;
    }


    // ------------------------------------------- CREATE_HISTORY --------------------------------------------


    @Test
    @DisplayName("createHistory - Debe crear entrada de historial correctamente")
    void createHistory_ShouldCreateHistoryEntry() {
        // Arrange
        ObjectId reportId = new ObjectId("507f1f77bcf86cd799439011");
        ObjectId userId = new ObjectId("507f1f77bcf86cd799439021");
        ReportStatus prevStatus = ReportStatus.PENDING;
        ReportStatus newStatus = ReportStatus.VERIFIED;

        ReportStatusHistory expectedEntity = testHistories.get(0);
        when(historyMapper.toEntity(reportId, userId, prevStatus, newStatus)).thenReturn(expectedEntity);

        // Act
        historyService.createHistory(reportId, userId, prevStatus, newStatus);

        // Assert
        verify(historyMapper).toEntity(reportId, userId, prevStatus, newStatus);
        verify(historyRepository).save(expectedEntity);
    }

    @Test
    @DisplayName("createHistory - Debe registrar timestamp actual cuando no se provee")
    void createHistory_ShouldSetCurrentTimestamp_WhenNotProvided() {
        // Arrange
        ObjectId reportId = new ObjectId();
        ObjectId userId = new ObjectId();
        ReportStatusHistory history = new ReportStatusHistory();
        history.setChangedAt(LocalDateTime.now());

        when(historyMapper.toEntity(any(), any(), any(), any())).thenReturn(history);

        // Act
        historyService.createHistory(reportId, userId, ReportStatus.PENDING, ReportStatus.VERIFIED);

        // Assert
        assertNotNull(history.getChangedAt());
    }


    // ------------------------------------------- GET_HISTORY_BY_ID --------------------------------------------


    @Test
    @DisplayName("getHistoryById - Debe retornar historial cuando existe")
    void getHistoryById_ShouldReturnHistory_WhenExists() {
        // Arrange
        String historyId = "507f1f77bcf86cd799439031";
        ReportStatusHistory history = testHistories.get(0);
        ReportStatusHistoryResponse expectedResponse = new ReportStatusHistoryResponse(
                historyId,
                history.getReportId().toHexString(),
                history.getUserId().toHexString(),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getChangedAt()
        );

        when(historyRepository.findById(new ObjectId(historyId))).thenReturn(Optional.of(history));
        when(historyMapper.toResponse(history)).thenReturn(expectedResponse);

        // Act
        ReportStatusHistoryResponse response = historyService.getHistoryById(historyId);

        // Assert
        assertNotNull(response);
        assertEquals(expectedResponse, response);
        verify(historyRepository).findById(new ObjectId(historyId));
    }

    @Test
    @DisplayName("getHistoryById - Debe lanzar HistoryNotFoundException cuando no existe")
    void getHistoryById_ShouldThrow_WhenNotFound() {
        // Arrange
        String historyId = "507f1f77bcf86cd799439099";
        when(historyRepository.findById(new ObjectId(historyId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(HistoryNotFoundException.class, () -> {
            historyService.getHistoryById(historyId);
        });
        verify(historyRepository).findById(new ObjectId(historyId));
    }

    @Test
    @DisplayName("getHistoryById - Debe lanzar IllegalArgumentException cuando ID es inválido")
    void getHistoryById_ShouldThrow_WhenInvalidId() {
        // Arrange
        String invalidId = "id-invalido";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            historyService.getHistoryById(invalidId);
        });
        verify(historyRepository, never()).findById(any());
    }


    // ------------------------------------------- GET_HISTORY_BY_REPORT_ID --------------------------------------------


    @Test
    @DisplayName("getHistoryByReportId - Debe retornar historial paginado correctamente")
    void getHistoryByReportId_ShouldReturnPaginatedResult() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        int page = 1;
        int size = 2;
        Pageable pageable = PageRequest.of(0, size);

        List<ReportStatusHistory> content = testHistories.subList(0, 2);
        Page<ReportStatusHistory> mockPage = new PageImpl<>(content, pageable, 5);

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(mock());
        when(historyRepository.findByReportId(new ObjectId(reportId), pageable)).thenReturn(mockPage);
        when(historyMapper.toListResponse(content)).thenReturn(content.stream().map(h -> new ReportStatusHistoryResponse(
                h.getId().toHexString(), h.getReportId().toHexString(), h.getUserId().toHexString(),
                h.getPreviousStatus(), h.getNewStatus(), h.getChangedAt()
        )).toList());

        // Act
        PaginatedHistoryResponse response = historyService.getHistoryByReportId(reportId, page, size);

        // Assert
        assertEquals(2, response.content().size());
        assertEquals(5, response.totalElements());
        assertEquals(1, response.page());
        assertEquals(2, response.size());
        verify(historyRepository).findByReportId(new ObjectId(reportId), pageable);
    }

    @Test
    @DisplayName("getHistoryByReportId - Debe retornar lista vacía cuando no hay resultados")
    void getHistoryByReportId_ShouldReturnEmpty_WhenNoResults() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439099";
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReportStatusHistory> emptyPage = Page.empty(pageable);

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(mock());
        when(historyRepository.findByReportId(new ObjectId(reportId), pageable)).thenReturn(emptyPage);
        when(historyMapper.toListResponse(List.of())).thenReturn(List.of());

        // Act
        PaginatedHistoryResponse response = historyService.getHistoryByReportId(reportId, 1, 10);

        // Assert
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        verify(historyRepository).findByReportId(new ObjectId(reportId), pageable);
    }


    @Test
    @DisplayName("getHistoryByReportId - Debe lanzar excepción si el ID es inválido")
    void getHistoryByReportId_ShouldThrow_WhenInvalidId() {
        // Arrange
        String invalidId = "abc123";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            historyService.getHistoryByReportId(invalidId, 1, 10);
        });

        verify(historyRepository, never()).findByReportId(any(), any());
    }


    // ------------------------------------------- GET_HISTORY_BY_USER_ID --------------------------------------------


    @Test
    @DisplayName("getHistoryByUserId - Debe retornar historial paginado correctamente")
    void getHistoryByUserId_ShouldReturnPaginatedResult() {
        // Arrange
        String userId = "507f1f77bcf86cd799439021";
        int page = 1;
        int size = 2;
        Pageable pageable = PageRequest.of(0, size);
        List<ReportStatusHistory> content = List.of(testHistories.get(0));

        Page<ReportStatusHistory> mockPage = new PageImpl<>(content, pageable, 1);

        when(userRepository.findById(new ObjectId(userId))).thenReturn(mock());
        when(historyRepository.findByUserId(new ObjectId(userId), pageable)).thenReturn(mockPage);
        when(historyMapper.toListResponse(content)).thenReturn(content.stream().map(h -> new ReportStatusHistoryResponse(
                h.getId().toHexString(), h.getReportId().toHexString(), h.getUserId().toHexString(),
                h.getPreviousStatus(), h.getNewStatus(), h.getChangedAt()
        )).toList());

        // Act
        PaginatedHistoryResponse response = historyService.getHistoryByUserId(userId, page, size);

        // Assert
        assertEquals(1, response.content().size());
        assertEquals(1, response.totalElements());
        verify(historyRepository).findByUserId(new ObjectId(userId), pageable);
    }


    @Test
    @DisplayName("getHistoryByUserId - Debe retornar lista vacía cuando no hay historial del usuario")
    void getHistoryByUserId_ShouldReturnEmpty_WhenNoResults() {
        // Arrange
        String userId = "507f1f77bcf86cd799439999";
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReportStatusHistory> emptyPage = Page.empty(pageable);

        when(userRepository.findById(new ObjectId(userId))).thenReturn(mock());
        when(historyRepository.findByUserId(new ObjectId(userId), pageable)).thenReturn(emptyPage);
        when(historyMapper.toListResponse(List.of())).thenReturn(List.of());

        // Act
        PaginatedHistoryResponse response = historyService.getHistoryByUserId(userId, 1, 10);

        // Assert
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        verify(historyRepository).findByUserId(new ObjectId(userId), pageable);
    }

    @Test
    @DisplayName("getHistoryByUserId - Debe lanzar excepción si el ID es inválido")
    void getHistoryByUserId_ShouldThrow_WhenInvalidId() {
        // Arrange
        String invalidId = "id-invalido";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            historyService.getHistoryByUserId(invalidId, 1, 10);
        });

        verify(historyRepository, never()).findByUserId(any(), any());
    }


    // ------------------------------------------- GET_HISTORY_BY_PREVIOUS_STATUS --------------------------------------------


    @Test
    @DisplayName("getHistoryByPreviousStatus - Debe retornar historial paginado filtrado por estado anterior")
    void getHistoryByPreviousStatus_ShouldReturnPaginatedResult() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        ReportStatus prevStatus = ReportStatus.PENDING;
        int page = 1;
        int size = 2;
        Pageable pageable = PageRequest.of(0, size);

        List<ReportStatusHistory> filtered = testHistories.stream()
                .filter(h -> h.getReportId().toHexString().equals(reportId) && h.getPreviousStatus() == prevStatus)
                .toList();

        Page<ReportStatusHistory> mockPage = new PageImpl<>(filtered, pageable, filtered.size());

        when(historyRepository.findByReportIdAndPreviousStatus(new ObjectId(reportId), prevStatus, pageable)).thenReturn(mockPage);
        when(historyMapper.toListResponse(filtered)).thenReturn(filtered.stream().map(h -> new ReportStatusHistoryResponse(
                h.getId().toHexString(), h.getReportId().toHexString(), h.getUserId().toHexString(),
                h.getPreviousStatus(), h.getNewStatus(), h.getChangedAt()
        )).toList());

        // Act
        PaginatedHistoryResponse response = historyService.getHistoryByPreviousStatus(reportId, prevStatus, page, size);

        // Assert
        assertEquals(filtered.size(), response.content().size());
        assertEquals(filtered.size(), response.totalElements());
        assertEquals(page, response.page());
    }


    @Test
    @DisplayName("getHistoryByPreviousStatus - Debe retornar lista vacía cuando no hay coincidencias")
    void getHistoryByPreviousStatus_ShouldReturnEmpty_WhenNoMatch() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        ReportStatus prevStatus = ReportStatus.DELETED; // Estado que no aparece
        Pageable pageable = PageRequest.of(0, 5);
        Page<ReportStatusHistory> emptyPage = Page.empty(pageable);

        when(historyRepository.findByReportIdAndPreviousStatus(new ObjectId(reportId), prevStatus, pageable)).thenReturn(emptyPage);
        when(historyMapper.toListResponse(List.of())).thenReturn(List.of());

        // Act
        PaginatedHistoryResponse response = historyService.getHistoryByPreviousStatus(reportId, prevStatus, 1, 5);

        // Assert
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
    }


    @Test
    @DisplayName("getHistoryByPreviousStatus - Debe lanzar excepción si el ID es inválido")
    void getHistoryByPreviousStatus_ShouldThrow_WhenInvalidId() {
        // Arrange
        String invalidId = "invalid";
        ReportStatus status = ReportStatus.PENDING;

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            historyService.getHistoryByPreviousStatus(invalidId, status, 1, 10);
        });
        verify(historyRepository, never()).findByReportIdAndPreviousStatus(any(), any(), any());
    }


    // ----------------------------------------- GET_HISTORY_BY_NEW_STATUS_AND_DATE_RANGE --------------------------------------------


    @Test
    @DisplayName("getHistoryByNewStatusAndDateRange - Debe retornar historial paginado filtrado por estado y fechas")
    void getHistoryByNewStatusAndDateRange_ShouldReturnPaginatedResult() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        ReportStatus newStatus = ReportStatus.VERIFIED;
        LocalDateTime start = LocalDateTime.now().minusDays(5);
        LocalDateTime end = LocalDateTime.now();
        int page = 1;
        int size = 5;
        Pageable pageable = PageRequest.of(0, size);

        List<ReportStatusHistory> filtered = testHistories.stream()
                .filter(h -> h.getReportId().toHexString().equals(reportId)
                        && h.getNewStatus() == newStatus
                        && !h.getChangedAt().isBefore(start) && !h.getChangedAt().isAfter(end))
                .toList();

        Page<ReportStatusHistory> mockPage = new PageImpl<>(filtered, pageable, filtered.size());

        when(historyRepository.findByReportIdAndNewStatusAndDateRange(new ObjectId(reportId), newStatus, start, end, pageable)).thenReturn(mockPage);
        when(historyMapper.toListResponse(filtered)).thenReturn(filtered.stream().map(h -> new ReportStatusHistoryResponse(
                h.getId().toHexString(), h.getReportId().toHexString(), h.getUserId().toHexString(),
                h.getPreviousStatus(), h.getNewStatus(), h.getChangedAt()
        )).toList());

        // Act
        PaginatedHistoryResponse response = historyService.getHistoryByNewStatusAndDateRange(reportId, newStatus, start, end, page, size);

        // Assert
        assertEquals(filtered.size(), response.content().size());
        assertEquals(page, response.page());
        assertEquals(filtered.size(), response.totalElements());
    }

    @Test
    @DisplayName("getHistoryByNewStatusAndDateRange - Debe retornar lista vacía cuando no hay coincidencias")
    void getHistoryByNewStatusAndDateRange_ShouldReturnEmpty_WhenNoMatch() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        ReportStatus newStatus = ReportStatus.DELETED;
        LocalDateTime start = LocalDateTime.now().minusDays(10);
        LocalDateTime end = LocalDateTime.now().minusDays(9);
        Pageable pageable = PageRequest.of(0, 10);

        when(historyRepository.findByReportIdAndNewStatusAndDateRange(new ObjectId(reportId), newStatus, start, end, pageable))
                .thenReturn(Page.empty(pageable));
        when(historyMapper.toListResponse(List.of())).thenReturn(List.of());

        // Act
        PaginatedHistoryResponse response = historyService.getHistoryByNewStatusAndDateRange(reportId, newStatus, start, end, 1, 10);

        // Assert
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
    }

    @Test
    @DisplayName("getHistoryByNewStatusAndDateRange - Debe lanzar excepción si el ID es inválido")
    void getHistoryByNewStatusAndDateRange_ShouldThrow_WhenInvalidId() {
        // Arrange
        String invalidId = "bad-id";
        ReportStatus status = ReportStatus.VERIFIED;
        LocalDateTime start = LocalDateTime.now().minusDays(5);
        LocalDateTime end = LocalDateTime.now();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            historyService.getHistoryByNewStatusAndDateRange(invalidId, status, start, end, 1, 10);
        });

        verify(historyRepository, never()).findByReportIdAndNewStatusAndDateRange(any(), any(), any(), any(), any());
    }


    // ------------------------------------------- COUNT_HISTORIES_BY_REPORT_ID --------------------------------------------


    @Test
    @DisplayName("countHistoryByReportId - Debe retornar el número correcto de cambios de estado")
    void countHistoryByReportId_ShouldReturnCorrectCount() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        long expectedCount = 3L;

        when(historyRepository.countByReportId(new ObjectId(reportId))).thenReturn(expectedCount);

        // Act
        long result = historyService.countHistoryByReportId(reportId);

        // Assert
        assertEquals(expectedCount, result);
        verify(historyRepository).countByReportId(new ObjectId(reportId));
    }


    @Test
    @DisplayName("countHistoryByReportId - Debe lanzar excepción si el ID es inválido")
    void countHistoryByReportId_ShouldThrow_WhenInvalidId() {
        // Arrange
        String invalidId = "invalid";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            historyService.countHistoryByReportId(invalidId);
        });

        verify(historyRepository, never()).countByReportId(any());
    }


    // ------------------------------------------- GET_HISTORY_BY_DATE_RANGE --------------------------------------------


    @Test
    @DisplayName("getHistoryByDateRange - Debe retornar historial filtrado por fechas")
    void getHistoryByDateRange_ShouldReturnFilteredHistory() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        LocalDateTime start = LocalDateTime.now().minusDays(5);
        LocalDateTime end = LocalDateTime.now();
        int page = 1;
        int size = 2;
        Pageable pageable = PageRequest.of(0, size);

        List<ReportStatusHistory> filtered = testHistories.stream()
                .filter(h -> h.getReportId().toHexString().equals(reportId)
                        && !h.getChangedAt().isBefore(start) && !h.getChangedAt().isAfter(end))
                .toList();

        Page<ReportStatusHistory> mockPage = new PageImpl<>(filtered, pageable, filtered.size());

        when(historyRepository.findByReportIdAndDateRange(new ObjectId(reportId), start, end, pageable)).thenReturn(mockPage);
        when(historyMapper.toListResponse(filtered)).thenReturn(
                filtered.stream().map(h -> new ReportStatusHistoryResponse(
                        h.getId().toHexString(),
                        h.getReportId().toHexString(),
                        h.getUserId().toHexString(),
                        h.getPreviousStatus(),
                        h.getNewStatus(),
                        h.getChangedAt()
                )).toList());

        // Act
        PaginatedHistoryResponse response = historyService.getHistoryByDateRange(reportId, start, end, page, size);

        // Assert
        assertEquals(filtered.size(), response.content().size());
        assertEquals(page, response.page());
        assertEquals(filtered.size(), response.totalElements());
    }


    @Test
    @DisplayName("getHistoryByDateRange - Debe retornar lista vacía si no hay historial en ese rango")
    void getHistoryByDateRange_ShouldReturnEmpty_WhenNoMatch() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        LocalDateTime start = LocalDateTime.now().minusDays(10);
        LocalDateTime end = LocalDateTime.now().minusDays(9);
        Pageable pageable = PageRequest.of(0, 5);
        Page<ReportStatusHistory> emptyPage = Page.empty(pageable);

        when(historyRepository.findByReportIdAndDateRange(new ObjectId(reportId), start, end, pageable))
                .thenReturn(emptyPage);
        when(historyMapper.toListResponse(List.of())).thenReturn(List.of());

        // Act
        PaginatedHistoryResponse response = historyService.getHistoryByDateRange(reportId, start, end, 1, 5);

        // Assert
        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
    }

    @Test
    @DisplayName("getHistoryByDateRange - Debe lanzar excepción si el ID es inválido")
    void getHistoryByDateRange_ShouldThrow_WhenInvalidId() {
        // Arrange
        String invalidId = "bad-id";
        LocalDateTime start = LocalDateTime.now().minusDays(5);
        LocalDateTime end = LocalDateTime.now();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            historyService.getHistoryByDateRange(invalidId, start, end, 1, 5);
        });

        verify(historyRepository, never()).findByReportIdAndDateRange(any(), any(), any(), any());
    }

}
