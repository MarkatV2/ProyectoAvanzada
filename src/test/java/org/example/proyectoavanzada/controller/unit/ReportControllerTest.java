package org.example.proyectoavanzada.controller.unit;

import co.edu.uniquindio.proyecto.controller.ReportController;
import co.edu.uniquindio.proyecto.controller.UserController;
import co.edu.uniquindio.proyecto.dto.comment.CommentPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.comment.CommentResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.report.PaginatedReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportRequest;
import co.edu.uniquindio.proyecto.dto.report.ReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusUpdate;
import co.edu.uniquindio.proyecto.entity.category.CategoryRef;
import co.edu.uniquindio.proyecto.exception.global.IdInvalidException;
import co.edu.uniquindio.proyecto.exception.global.ServiceUnavailableException;
import co.edu.uniquindio.proyecto.exception.report.DuplicateReportException;
import co.edu.uniquindio.proyecto.exception.report.ReportNotFoundException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import co.edu.uniquindio.proyecto.exceptionhandler.global.GlobalExceptionHandler;
import co.edu.uniquindio.proyecto.exceptionhandler.report.ReportExceptionHandler;
import co.edu.uniquindio.proyecto.exceptionhandler.user.UserExceptionHandler;
import co.edu.uniquindio.proyecto.service.interfaces.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.example.proyectoavanzada.configuration.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@ContextConfiguration(classes = {ReportController.class, TestSecurityConfig.class})
@Import({ReportExceptionHandler.class, ErrorResponseBuilder.class, GlobalExceptionHandler.class})
public class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ReportService reportService;
    @Autowired
    private ObjectMapper objectMapper;
    private List<ReportResponse> mockReports;
    private List<ImageResponse> mockImages;
    private CommentPaginatedResponse mockCommentsResponse;

    @BeforeEach
    void setUp() {
        // Reportes simulados (5 elementos)
        mockReports = List.of(
                new ReportResponse(new ObjectId().toHexString(), "Falla eléctrica", "Sin luz", List.of(), 4.0, -75.7, "OPEN", LocalDateTime.now(), 2, "user1"),
                new ReportResponse(new ObjectId().toHexString(), "Hueco",         "Hueco en vía",         List.of(), 4.01, -75.72, "OPEN",       LocalDateTime.now(), 1, "user2"),
                new ReportResponse(new ObjectId().toHexString(), "Accidente",     "Choque leve",          List.of(), 4.03, -75.74, "CLOSED",     LocalDateTime.now(), 0, "user3"),
                new ReportResponse(new ObjectId().toHexString(), "Basura",        "Acumulación de basura", List.of(), 4.04, -75.73, "OPEN",       LocalDateTime.now(), 3, "user4"),
                new ReportResponse(new ObjectId().toHexString(), "Ruido",         "Ruido excesivo",       List.of(), 4.02, -75.71, "IN_PROGRESS",LocalDateTime.now(), 5, "user5")
        );

        // Imágenes simuladas para un reporte
        mockImages = List.of(
                new ImageResponse("img1", "http://example.com/img1.jpg", LocalDateTime.now().minusDays(1)),
                new ImageResponse("img2", "http://example.com/img2.jpg", LocalDateTime.now().minusHours(5)),
                new ImageResponse("img3", "http://example.com/img3.jpg", LocalDateTime.now().minusMinutes(30))
        );

        // Comentarios simulados y respuesta paginada
        List<CommentResponse> mockComments = List.of(
                new CommentResponse("c1", "Alice", "uA", mockReports.get(0).id(), "Buen reporte", LocalDateTime.now().minusDays(2)),
                new CommentResponse("c2", "Bob",   "uB", mockReports.get(0).id(), "Necesita atención", LocalDateTime.now().minusHours(10))
        );
        // Página 1, tamaño 2, total elementos = 2, total páginas = 1
        mockCommentsResponse = new CommentPaginatedResponse(
                mockComments,
                1,
                2,
                mockComments.size(),
                1
        );
    }


    @Test
    @DisplayName("GET /api/v1/reports debe retornar 200 y una lista de reportes cercanos")
    void testGetReportsSuccess() throws Exception {
        PaginatedReportResponse response = new PaginatedReportResponse(
                mockReports, 1, 5, 5, 1
        );

        when(reportService.getReportsNearLocation(anyDouble(), anyDouble(), any(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/reports")
                        .param("latitud", "4.0")
                        .param("longitud", "-75.7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/reports debe retornar 400 si los parámetros son inválidos")
    void testGetReportsBadRequest_InvalidParams() throws Exception {
        // Latitude no numérico → parámetro inválido
        mockMvc.perform(get("/api/v1/reports")
                        .param("latitud", "abc")
                        .param("longitud", "-75.7"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/reports debe retornar 400 si el servicio lanza IllegalArgumentException")
    void testGetReportsBadRequest_IllegalArgument() throws Exception {
        when(reportService.getReportsNearLocation(anyDouble(), anyDouble(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Latitud inválida"));

        mockMvc.perform(get("/api/v1/reports")
                        .param("latitud", "-200")  // fuera del rango
                        .param("longitud", "-75.7"))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("GET /api/v1/reports debe retornar 200 incluso con parámetros opcionales ausentes")
    void testGetReports_WithMinimalParams() throws Exception {
        when(reportService.getReportsNearLocation(eq(4.0), eq(-75.7), any(), any(), any(), any()))
                .thenReturn(new PaginatedReportResponse(mockReports, 1, 5, 5, 1));

        mockMvc.perform(get("/api/v1/reports")
                        .param("latitud", "4.0")
                        .param("longitud", "-75.7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)));
    }

    @Test
    @DisplayName("GET /api/v1/reports/{id} retorna 200 cuando el reporte existe")
    void testGetReport_Success() throws Exception {
        // Arrange
        ReportResponse rr = mockReports.get(0);
        when(reportService.getReportById(rr.id())).thenReturn(rr);

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/{id}", rr.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rr.id()))
                .andExpect(jsonPath("$.title").value(rr.title()))
                .andExpect(jsonPath("$.reportStatus").value(rr.reportStatus()));
    }

    @Test
    @DisplayName("GET /api/v1/reports/{id} retorna 404 cuando el reporte no existe")
    void testGetReport_NotFound() throws Exception {
        // Arrange
        when(reportService.getReportById("no-id")).thenThrow(new ReportNotFoundException("no-id"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/{id}", "no-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/reports/{id} retorna 400 cuando el ID tiene formato inválido")
    void testGetReport_IdInvalid() throws Exception {
        // Arrange
        when(reportService.getReportById("xxx")).thenThrow(new IdInvalidException("xxx"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/{id}", "xxx"))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("POST /api/v1/reports retorna 201 y Location cuando la petición es válida")
    void testCreateReport_Success() throws Exception {
        // Arrange
        ReportRequest req = new ReportRequest("Título","Descripción", List.of(new CategoryRef("cat1")), 4.0, -75.0);
        ReportResponse rr = new ReportResponse("newId", req.title(), req.description(), req.categoryList(),
                req.latitude(), req.longitude(), "OPEN", LocalDateTime.now(), 0, "u1");
        when(reportService.createReport(any(ReportRequest.class))).thenReturn(rr);

        // Act & Assert
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/reports/newId")))
                .andExpect(jsonPath("$.id").value("newId"))
                .andExpect(jsonPath("$.title").value("Título"));
    }

    @Test
    @DisplayName("POST /api/v1/reports retorna 409 cuando el reporte ya existe")
    void testCreateReport_Duplicated() throws Exception {
        // Arrange
        when(reportService.createReport(any(ReportRequest.class)))
                .thenThrow(new DuplicateReportException("dup"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReportRequest("T","D", List.of(new CategoryRef("c")), 4.0, -75.0))))
                .andExpect(status().isConflict());
    }


    @Test
    @DisplayName("POST /api/v1/reports retorna 400 cuando faltan datos obligatorios")
    void testCreateReport_FailedValidated() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", hasSize(3)));
    }


    @Test
    @DisplayName("PUT /api/v1/reports/{id} retorna 200 cuando el reporte se actualiza correctamente")
    void testUpdateReport_Success() throws Exception {
        // Arrange
        ReportResponse rr = mockReports.get(0);
        ReportRequest req = new ReportRequest("Nuevo título", "Nueva descripción", List.of(new CategoryRef("c1")), 4.5, -75.5);
        when(reportService.updateReport(eq(rr.id()), any(ReportRequest.class)))
                .thenReturn(rr);

        // Act & Assert
        mockMvc.perform(put("/api/v1/reports/{id}", rr.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/api/v1/reports/" + rr.id()))
                .andExpect(jsonPath("$.id").value(rr.id()))
                .andExpect(jsonPath("$.title").value(rr.title()));
    }

    @Test
    @DisplayName("PUT /api/v1/reports/{id} retorna 404 cuando el reporte no existe")
    void testUpdateReport_NotFound() throws Exception {
        // Arrange
        String badId = "no-id";
        ReportRequest req = new ReportRequest("Nuevo título", "Nueva descripción",
                List.of(new CategoryRef("c1")), 4.5, -75.5);
        when(reportService.updateReport(eq(badId), any(ReportRequest.class)))
                .thenThrow(new ReportNotFoundException(badId));

        // Act & Assert
        mockMvc.perform(put("/api/v1/reports/{id}", badId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/reports/{id} retorna 400 cuando el ID tiene formato inválido")
    void testUpdateReport_InvalidId() throws Exception {
        // Arrange
        String invalidId = "xxx";
        when(reportService.updateReport(eq(invalidId), any(ReportRequest.class)))
                .thenThrow(new IdInvalidException(invalidId));

        // Act & Assert
        mockMvc.perform(put("/api/v1/reports/{id}", invalidId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/reports/{id} retorna 400 cuando faltan datos obligatorios")
    void testUpdateReport_ValidationFailed() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/v1/reports/{id}", mockReports.get(0).id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("DELETE /api/v1/reports/{id} retorna 204 cuando se elimina correctamente")
    void testDeleteReport_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/reports/{id}", mockReports.get(0).id()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/reports/{id} retorna 404 cuando el reporte no existe")
    void testDeleteReport_NotFound() throws Exception {
        // Arrange
        String badId = "no-id";
        doThrow(new ReportNotFoundException(badId))
                .when(reportService).softDeleteReport(badId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/reports/{id}", badId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/reports/{id} retorna 400 cuando el ID tiene formato inválido")
    void testDeleteReport_InvalidId() throws Exception {
        // Arrange
        String invalidId = "xxx";
        doThrow(new IdInvalidException(invalidId))
                .when(reportService).softDeleteReport(invalidId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/reports/{id}", invalidId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/reports/{id}/status retorna 200 cuando el estado se actualiza correctamente")
    void testUpdateReportStatus_Success() throws Exception {
        // Arrange
        String id = mockReports.get(0).id();
        ReportStatusUpdate dto = new ReportStatusUpdate("CLOSED", "Motivo de rechazo");
        doNothing().when(reportService).updateReportStatus(eq(id), any(ReportStatusUpdate.class));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/reports/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/v1/reports/{id}/status retorna 404 cuando el reporte no existe")
    void testUpdateReportStatus_NotFound() throws Exception {
        // Arrange
        String badId = "no-id";
        ReportStatusUpdate dto = new ReportStatusUpdate("OPEN", null);
        doThrow(new ReportNotFoundException(badId))
                .when(reportService).updateReportStatus(eq(badId), any());

        // Act & Assert
        mockMvc.perform(patch("/api/v1/reports/{id}/status", badId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/v1/reports/{id}/status retorna 400 cuando el ID es inválido")
    void testUpdateReportStatus_InvalidId() throws Exception {
        // Arrange
        String invalidId = "xxx";
        ReportStatusUpdate dto = new ReportStatusUpdate("OPEN", null);
        doThrow(new IdInvalidException(invalidId))
                .when(reportService).updateReportStatus(eq(invalidId), any());

        // Act & Assert
        mockMvc.perform(patch("/api/v1/reports/{id}/status", invalidId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/reports/{id}/status retorna 400 cuando la transición es inválida")
    void testUpdateReportStatus_BadRequestOnIllegalArgument() throws Exception {
        // Arrange
        String id = mockReports.get(0).id();
        ReportStatusUpdate dto = new ReportStatusUpdate("INVALID", null);
        doThrow(new IllegalArgumentException("Estado no permitido"))
                .when(reportService).updateReportStatus(eq(id), any());

        // Act & Assert
        mockMvc.perform(patch("/api/v1/reports/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("PATCH /api/v1/reports/{id}/votes retorna 204 cuando el voto se alterna correctamente")
    void testToggleVote_Success() throws Exception {
        // Arrange
        String id = mockReports.get(0).id();
        doNothing().when(reportService).toggleReportVote(id);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/reports/{id}/votes", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /api/v1/reports/{id}/votes retorna 404 cuando el reporte no existe")
    void testToggleVote_NotFound() throws Exception {
        // Arrange
        String badId = "no-id";
        doThrow(new ReportNotFoundException(badId))
                .when(reportService).toggleReportVote(badId);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/reports/{id}/votes", badId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/v1/reports/{id}/votes retorna 400 cuando el ID es inválido")
    void testToggleVote_InvalidId() throws Exception {
        // Arrange
        String invalidId = "xxx";
        doThrow(new IdInvalidException(invalidId))
                .when(reportService).toggleReportVote(invalidId);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/reports/{id}/votes", invalidId))
                .andExpect(status().isBadRequest());
    }


    // Imágenes del reporte
    @Test
    @DisplayName("GET /api/v1/reports/{id}/images retorna 200 con lista de imágenes")
    void testGetAllImagesByReport_Success() throws Exception {
        // Arrange
        String id = mockReports.get(0).id();
        when(reportService.getAllImagesByReport(id)).thenReturn(mockImages);

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/{id}/images", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(mockImages.size())))
                .andExpect(jsonPath("$[0].id").value(mockImages.get(0).id()))
                .andExpect(jsonPath("$[0].imageUrl").value(mockImages.get(0).imageUrl()));
    }

    @Test
    @DisplayName("GET /api/v1/reports/{id}/images retorna 404 cuando el reporte no existe")
    void testGetAllImagesByReport_NotFound() throws Exception {
        // Arrange
        String badId = "no-id";
        when(reportService.getAllImagesByReport(badId))
                .thenThrow(new ReportNotFoundException(badId));

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/{id}/images", badId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/reports/{id}/images retorna 400 cuando el ID es inválido")
    void testGetAllImagesByReport_InvalidId() throws Exception {
        // Arrange
        String invalidId = "xxx";
        when(reportService.getAllImagesByReport(invalidId))
                .thenThrow(new IdInvalidException(invalidId));

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/{id}/images", invalidId))
                .andExpect(status().isBadRequest());
    }


    // Comentarios del reporte
    @Test
    @DisplayName("GET /api/v1/reports/{id}/comments retorna 200 con comentarios paginados")
    void testGetCommentsByReport_Success() throws Exception {
        // Arrange
        String id = mockReports.get(0).id();
        int page = 1, size = 2;
        when(reportService.getCommentsByReportId(id, page, size))
                .thenReturn(mockCommentsResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/{id}/comments", id)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(mockCommentsResponse.content().size())))
                .andExpect(jsonPath("$.page").value(mockCommentsResponse.page()))
                .andExpect(jsonPath("$.size").value(mockCommentsResponse.size()))
                .andExpect(jsonPath("$.totalElements").value(mockCommentsResponse.totalElements()))
                .andExpect(jsonPath("$.totalPages").value(mockCommentsResponse.totalPages()));
    }

    @Test
    @DisplayName("GET /api/v1/reports/{id}/comments retorna 404 cuando el reporte no existe")
    void testGetCommentsByReport_NotFound() throws Exception {
        // Arrange
        String badId = "no-id";
        when(reportService.getCommentsByReportId(badId, 0, 10))
                .thenThrow(new ReportNotFoundException(badId));

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/{id}/comments", badId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/reports/{id}/comments retorna 400 cuando el ID es inválido")
    void testGetCommentsByReport_InvalidId() throws Exception {
        // Arrange
        String invalidId = "xxx";
        when(reportService.getCommentsByReportId(invalidId, 0, 10))
                .thenThrow(new IdInvalidException(invalidId));

        // Act & Assert
        mockMvc.perform(get("/api/v1/reports/{id}/comments", invalidId))
                .andExpect(status().isBadRequest());
    }

}

