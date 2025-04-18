package org.example.proyectoavanzada.controller.unit;


import co.edu.uniquindio.proyecto.controller.ReportStatusHistoryController;
import co.edu.uniquindio.proyecto.dto.report.PaginatedHistoryResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusHistoryResponse;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.exception.report.HistoryNotFoundException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import co.edu.uniquindio.proyecto.exceptionhandler.global.GlobalExceptionHandler;
import co.edu.uniquindio.proyecto.exceptionhandler.report.ReportExceptionHandler;
import co.edu.uniquindio.proyecto.service.implementations.ReportStatusHistoryServiceImpl;
import org.bson.types.ObjectId;
import org.example.proyectoavanzada.configuration.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = ReportStatusHistoryController.class)
@Import({ReportExceptionHandler.class, GlobalExceptionHandler.class, ErrorResponseBuilder.class})
@ContextConfiguration(classes = {ReportStatusHistoryController.class, TestSecurityConfig.class})
class ReportStatusHistoryControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportStatusHistoryServiceImpl historyService;

    private List<ReportStatusHistoryResponse> allHistories;

    @BeforeEach
    void setUp() {
        // Crear 5 historiales de ejemplo
        allHistories = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> new ReportStatusHistoryResponse(
                        new ObjectId().toHexString(),
                        new ObjectId().toHexString(),
                        new ObjectId().toHexString(),
                        i % 2 == 0 ? ReportStatus.VERIFIED : ReportStatus.PENDING,
                        i % 2 == 0 ? ReportStatus.RESOLVED : ReportStatus.VERIFIED,
                        LocalDateTime.now().minusDays(i)
                ))
                .toList();
    }

    // -------------------------------- getHistoryById --------------------------------

    @Test
    @DisplayName("GET /api/v1/report-status-histories/{id} retorna 200 y el historial cuando existe")
    void getHistoryById_Success() throws Exception {
        var hist = allHistories.get(0);
        when(historyService.getHistoryById(hist.id())).thenReturn(hist);

        mockMvc.perform(get("/api/v1/report-status-histories/{id}", hist.id()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(hist.id()))
                .andExpect(jsonPath("$.reportId").value(hist.reportId()))
                .andExpect(jsonPath("$.previousStatus").value(hist.previousStatus().toString()))
                .andExpect(jsonPath("$.newStatus").value(hist.newStatus().toString()));

        verify(historyService).getHistoryById(hist.id());
    }

    @Test
    @DisplayName("GET /api/v1/report-status-histories/{id} retorna 404 cuando no existe")
    void getHistoryById_NotFound() throws Exception {
        String badId = new ObjectId().toHexString();
        when(historyService.getHistoryById(badId))
                .thenThrow(new HistoryNotFoundException("Historial no encontrado"));

        mockMvc.perform(get("/api/v1/report-status-histories/{id}", badId))
                .andExpect(status().isNotFound());

        verify(historyService).getHistoryById(badId);
    }

    @Test
    @DisplayName("GET /api/v1/report-status-histories/{id} retorna 400 cuando el ID es inválido")
    void getHistoryById_InvalidId() throws Exception {
        String invalid = "abc123";

        when(historyService.getHistoryById(invalid))
                .thenThrow(new IllegalArgumentException("ID inválido"));

        mockMvc.perform(get("/api/v1/report-status-histories/{id}", invalid))
                .andExpect(status().isBadRequest());

        verify(historyService).getHistoryById(invalid);
    }

    // ----------------------------- getByReportId -----------------------------

    @Test
    @DisplayName("GET /api/v1/report-status-histories/by-report retorna 200 con paginación válida")
    void getByReportId_Success() throws Exception {
        String reportId = allHistories.get(0).reportId();
        int page = 2, size = 2;
        List<ReportStatusHistoryResponse> pageContent = allHistories.subList(2, 4);
        long total = allHistories.size();
        int totalPages = (int) Math.ceil((double) total / size);

        PaginatedHistoryResponse resp =
                new PaginatedHistoryResponse(pageContent, page, size, total, totalPages);

        when(historyService.getHistoryByReportId(reportId, page, size)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/report-status-histories/by-report")
                        .param("reportId", reportId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(page))
                .andExpect(jsonPath("$.size").value(size))
                .andExpect(jsonPath("$.totalElements").value(total))
                .andExpect(jsonPath("$.totalPages").value(totalPages))
                .andExpect(jsonPath("$.content.length()").value(pageContent.size()));

        verify(historyService).getHistoryByReportId(reportId, page, size);
    }


    @Test
    @DisplayName("GET /api/v1/report-status-histories/by-report retorna lista vacía si no hay datos")
    void getByReportId_Empty() throws Exception {
        String reportId = new ObjectId().toHexString();
        // Coincide con el defaultValue="20" del controlador:
        PaginatedHistoryResponse emptyResp =
                new PaginatedHistoryResponse(List.of(), 1, 20, 0, 0);

        // Stub para page=1, size=20
        when(historyService.getHistoryByReportId(reportId, 1, 20))
                .thenReturn(emptyResp);

        mockMvc.perform(get("/api/v1/report-status-histories/by-report")
                        .param("reportId", reportId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.page").value(1));

        verify(historyService).getHistoryByReportId(reportId, 1, 20);
    }

    @Test
    @DisplayName("GET /api/v1/report-status-histories/by-report retorna 400 cuando el ID es inválido")
    void getByReportId_InvalidId() throws Exception {
        String invalid = "not-an-objectid";

        when(historyService.getHistoryByReportId(invalid, 1, 20))
                .thenThrow(new IllegalArgumentException("ID inválido"));

        mockMvc.perform(get("/api/v1/report-status-histories/by-report")
                        .param("reportId", invalid))
                .andExpect(status().isBadRequest());

        verify(historyService).getHistoryByReportId(invalid, 1, 20);
    }


    // ------------------------ getByDateRange ------------------------

    @Test
    @DisplayName("GET /by-report/date-range retorna 200 con resultados paginados")
    void getByDateRange_Success() throws Exception {
        String reportId = allHistories.get(0).reportId();
        String start = "2025-04-10T00:00:00";
        String end   = "2025-04-17T23:59:59";
        int page = 2, size = 2;

        List<ReportStatusHistoryResponse> pageContent = allHistories.subList(2, 4);
        long total = allHistories.size();
        int totalPages = (int) Math.ceil((double) total / size);

        PaginatedHistoryResponse resp =
                new PaginatedHistoryResponse(pageContent, page, size, total, totalPages);

        when(historyService.getHistoryByDateRange(reportId,
                LocalDateTime.parse(start),
                LocalDateTime.parse(end),
                page, size))
                .thenReturn(resp);

        mockMvc.perform(get("/api/v1/report-status-histories/by-report/date-range")
                        .param("reportId", reportId)
                        .param("startDate", start)
                        .param("endDate", end)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(page))
                .andExpect(jsonPath("$.size").value(size))
                .andExpect(jsonPath("$.totalElements").value(total))
                .andExpect(jsonPath("$.totalPages").value(totalPages))
                .andExpect(jsonPath("$.content.length()").value(pageContent.size()));

        verify(historyService).getHistoryByDateRange(reportId,
                LocalDateTime.parse(start),
                LocalDateTime.parse(end),
                page, size);
    }

    @Test
    @DisplayName("GET /by-report/date-range retorna 400 cuando ID inválido")
    void getByDateRange_InvalidId() throws Exception {
        String invalidId = "bad-id";
        String start = "2025-04-10T00:00:00";
        String end   = "2025-04-17T23:59:59";

        when(historyService.getHistoryByDateRange(eq(invalidId), any(), any(), anyInt(), anyInt()))
                .thenThrow(new IllegalArgumentException("ID inválido"));

        mockMvc.perform(get("/api/v1/report-status-histories/by-report/date-range")
                        .param("reportId", invalidId)
                        .param("startDate", start)
                        .param("endDate", end))
                .andExpect(status().isBadRequest());

        verify(historyService).getHistoryByDateRange(eq(invalidId),
                any(LocalDateTime.class), any(LocalDateTime.class), eq(1), eq(20));
    }

    @Test
    @DisplayName("GET /by-report/date-range retorna 400 cuando faltan parámetros obligatorios")
    void getByDateRange_MissingParams() throws Exception {
        mockMvc.perform(get("/api/v1/report-status-histories/by-report/date-range")
                                .param("reportId", allHistories.get(0).reportId())
                        // falta startDate y endDate
                )
                .andExpect(status().isBadRequest());
        verifyNoInteractions(historyService);
    }

    // ---------------------- getByPreviousStatus ----------------------

    @Test
    @DisplayName("GET /by-report/previous-status retorna 200 con resultados filtrados")
    void getByPreviousStatus_Success() throws Exception {
        String reportId = allHistories.get(0).reportId();
        ReportStatus prev = ReportStatus.PENDING;
        int page = 1, size = 3;

        List<ReportStatusHistoryResponse> filtered = allHistories.stream()
                .filter(h -> h.previousStatus() == prev)
                .toList();
        long total = filtered.size();
        int totalPages = 1;

        PaginatedHistoryResponse resp =
                new PaginatedHistoryResponse(filtered, page, size, total, totalPages);

        when(historyService.getHistoryByPreviousStatus(reportId, prev, page, size))
                .thenReturn(resp);

        mockMvc.perform(get("/api/v1/report-status-histories/by-report/previous-status")
                        .param("reportId", reportId)
                        .param("previousStatus", prev.toString())
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(page))
                .andExpect(jsonPath("$.size").value(size))
                .andExpect(jsonPath("$.totalElements").value(total))
                .andExpect(jsonPath("$.content.length()").value(filtered.size()));

        verify(historyService).getHistoryByPreviousStatus(reportId, prev, page, size);
    }

    @Test
    @DisplayName("GET /by-report/previous-status retorna 400 cuando enum inválido")
    void getByPreviousStatus_InvalidEnum() throws Exception {
        mockMvc.perform(get("/api/v1/report-status-histories/by-report/previous-status")
                        .param("reportId", allHistories.get(0).reportId())
                        .param("previousStatus", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(historyService);
    }

    @Test
    @DisplayName("GET /by-report/previous-status retorna 400 cuando falta previousStatus")
    void getByPreviousStatus_MissingParam() throws Exception {
        mockMvc.perform(get("/api/v1/report-status-histories/by-report/previous-status")
                        .param("reportId", allHistories.get(0).reportId()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(historyService);
    }

    // -------------------------------- getByNewStatusAndDateRange --------------------------------

    @Test
    @DisplayName("GET /by-report/new-status-and-dates retorna 200 con resultados filtrados")
    void getByNewStatusAndDateRange_Success() throws Exception {
        String reportId = allHistories.get(0).reportId();
        ReportStatus newStatus = ReportStatus.RESOLVED;
        String start = "2025-04-10T00:00:00";
        String end   = "2025-04-17T23:59:59";
        int page = 1, size = 3;

        List<ReportStatusHistoryResponse> filtered = allHistories.stream()
                .filter(h -> h.newStatus() == newStatus &&
                        !h.changedAt().isBefore(LocalDateTime.parse(start)) &&
                        !h.changedAt().isAfter(LocalDateTime.parse(end)))
                .toList();
        long total = filtered.size();
        int totalPages = (int) Math.ceil((double) total / size);

        PaginatedHistoryResponse resp =
                new PaginatedHistoryResponse(filtered, page, size, total, totalPages);

        when(historyService.getHistoryByNewStatusAndDateRange(
                reportId,
                newStatus,
                LocalDateTime.parse(start),
                LocalDateTime.parse(end),
                page, size))
                .thenReturn(resp);

        mockMvc.perform(get("/api/v1/report-status-histories/by-report/new-status-and-dates")
                        .param("reportId", reportId)
                        .param("newStatus", newStatus.toString())
                        .param("startDate", start)
                        .param("endDate", end)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(page))
                .andExpect(jsonPath("$.size").value(size))
                .andExpect(jsonPath("$.totalElements").value(total))
                .andExpect(jsonPath("$.totalPages").value(totalPages))
                .andExpect(jsonPath("$.content.length()").value(filtered.size()));

        verify(historyService).getHistoryByNewStatusAndDateRange(
                reportId,
                newStatus,
                LocalDateTime.parse(start),
                LocalDateTime.parse(end),
                page, size);
    }

    @Test
    @DisplayName("GET /by-report/new-status-and-dates retorna 400 cuando enum inválido")
    void getByNewStatusAndDateRange_InvalidEnum() throws Exception {
        mockMvc.perform(get("/api/v1/report-status-histories/by-report/new-status-and-dates")
                        .param("reportId", allHistories.get(0).reportId())
                        .param("newStatus", "NOT_A_STATUS")
                        .param("startDate", "2025-04-10T00:00:00")
                        .param("endDate", "2025-04-17T23:59:59"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(historyService);
    }

    @Test
    @DisplayName("GET /by-report/new-status-and-dates retorna 400 cuando falta parámetro obligatorio")
    void getByNewStatusAndDateRange_MissingParam() throws Exception {
        mockMvc.perform(get("/api/v1/report-status-histories/by-report/new-status-and-dates")
                        .param("reportId", allHistories.get(0).reportId())
                        .param("newStatus", ReportStatus.PENDING.toString()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(historyService);
    }

    // -------------------------------- getByUserId --------------------------------

    @Test
    @DisplayName("GET /by-user retorna 200 con resultados paginados")
    void getByUserId_Success() throws Exception {
        String userId = allHistories.get(0).userId();
        int page = 2, size = 2;
        List<ReportStatusHistoryResponse> pageContent = allHistories.subList(2, 4);
        long total = allHistories.size();
        int totalPages = (int) Math.ceil((double) total / size);

        PaginatedHistoryResponse resp =
                new PaginatedHistoryResponse(pageContent, page, size, total, totalPages);

        when(historyService.getHistoryByUserId(userId, page, size)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/report-status-histories/by-user")
                        .param("userId", userId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(page))
                .andExpect(jsonPath("$.size").value(size))
                .andExpect(jsonPath("$.totalElements").value(total))
                .andExpect(jsonPath("$.totalPages").value(totalPages))
                .andExpect(jsonPath("$.content.length()").value(pageContent.size()));

        verify(historyService).getHistoryByUserId(userId, page, size);
    }

    @Test
    @DisplayName("GET /by-user retorna 400 cuando ID inválido")
    void getByUserId_InvalidId() throws Exception {
        String invalid = "not-an-oid";
        when(historyService.getHistoryByUserId(invalid, 1, 20))
                .thenThrow(new IllegalArgumentException("ID inválido"));

        mockMvc.perform(get("/api/v1/report-status-histories/by-user")
                        .param("userId", invalid))
                .andExpect(status().isBadRequest());

        verify(historyService).getHistoryByUserId(invalid, 1, 20);
    }


    @Test
    @DisplayName("GET /by-user retorna 400 cuando falta parámetro userId")
    void getByUserId_MissingParam() throws Exception {
        mockMvc.perform(get("/api/v1/report-status-histories/by-user"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(historyService);
    }


    @Test
    @DisplayName("GET /api/v1/report-status-histories/count retorna 200 y la cuenta correcta")
    void countByReportId_Success() throws Exception {
        String reportId = allHistories.get(0).reportId();
        when(historyService.countHistoryByReportId(reportId)).thenReturn(1l);

        mockMvc.perform(get("/api/v1/report-status-histories/count")
                        .param("reportId", reportId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().string(String.valueOf(1)));

        verify(historyService).countHistoryByReportId(reportId);
    }

    @Test
    @DisplayName("GET /api/v1/report-status-histories/count retorna 400 cuando el ID es inválido")
    void countByReportId_InvalidId() throws Exception {
        String invalidId = "bad-id";
        when(historyService.countHistoryByReportId(invalidId))
                .thenThrow(new IllegalArgumentException("ID inválido"));

        mockMvc.perform(get("/api/v1/report-status-histories/count")
                        .param("reportId", invalidId))
                .andExpect(status().isBadRequest());

        verify(historyService).countHistoryByReportId(invalidId);
    }

    @Test
    @DisplayName("GET /api/v1/report-status-histories/count retorna 400 cuando falta reportId")
    void countByReportId_MissingParam() throws Exception {
        mockMvc.perform(get("/api/v1/report-status-histories/count"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(historyService);
    }
}

