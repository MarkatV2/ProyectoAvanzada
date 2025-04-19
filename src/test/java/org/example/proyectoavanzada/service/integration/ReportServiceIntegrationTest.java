package org.example.proyectoavanzada.service.integration;


import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.dto.comment.CommentPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.report.*;
import co.edu.uniquindio.proyecto.entity.category.CategoryRef;
import co.edu.uniquindio.proyecto.entity.comment.Comment;
import co.edu.uniquindio.proyecto.entity.comment.CommentStatus;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.entity.report.ReportStatusHistory;
import co.edu.uniquindio.proyecto.exception.global.IdInvalidException;
import co.edu.uniquindio.proyecto.exception.report.DuplicateReportException;
import co.edu.uniquindio.proyecto.exception.report.ReportNotFoundException;
import co.edu.uniquindio.proyecto.repository.CommentRepository;
import co.edu.uniquindio.proyecto.repository.ReportRepository;
import co.edu.uniquindio.proyecto.repository.ReportStatusHistoryRepository;
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.service.interfaces.CommentService;
import co.edu.uniquindio.proyecto.service.interfaces.ImageService;
import co.edu.uniquindio.proyecto.service.interfaces.ReportService;
import co.edu.uniquindio.proyecto.service.interfaces.ReportStatusHistoryService;
import co.edu.uniquindio.proyecto.service.mapper.ReportMapper;
import co.edu.uniquindio.proyecto.util.SecurityUtils;
import co.edu.uniquindio.proyecto.validator.ReportStatusChangeRequestValidator;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ContextConfiguration(classes = ProyectoApplication.class)
class ReportServiceIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    ReportStatusHistoryService reportStatusHistoryService;
    @Autowired
    CommentRepository commentRepository;

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ReportStatusHistoryRepository historyRepository;
    @MockitoBean
    private EmailService emailService; // Mock para evitar dependencias de correo
    @MockitoBean
    private SecurityUtils securityUtils;


    private List<Report> existing;

    @BeforeEach
    void setUp() {
        // Limpiar la colección
        mongoTemplate.getCollection("reports").drop();
        mongoTemplate.getCollection("images").drop();
        mongoTemplate.getCollection("comments").drop();

        // 2) Asegurar índice 2dsphere en reports.location
        mongoTemplate.indexOps(Report.class)
                .ensureIndex(new GeospatialIndex("location")
                        .typed(GeoSpatialIndexType.GEO_2DSPHERE));

        Mockito.doNothing().when(emailService).sendCommentEmail(any(), any(), any(), any());

        // Insertar 5 reportes de ejemplo
        existing = IntStream.rangeClosed(1, 5).mapToObj(i -> {
            Report r = new Report();
            r.setId(new ObjectId());
            r.setTitle("Title" + i);
            r.setDescription("Desc" + i);
            r.setCategoryList(List.of(new CategoryRef("cat" + i)));
            r.setLocation(new GeoJsonPoint(10 +  (i * 0.001), 10 +  (i * 0.001)));
            r.setUserEmail("user" + i + "@example.com");
            r.setReportStatus(ReportStatus.VERIFIED);
            r.setImportantVotes(i);
            r.setUserId(new ObjectId());
            r.setCreatedAt(LocalDateTime.now().minusDays(i));
            return r;
        }).toList();

        // 4) Insertar 5 imágenes de ejemplo para el primer reporte
        ObjectId firstReportId = existing.get(0).getId();
        List<Document> images = IntStream.rangeClosed(1, 5).mapToObj(i ->
                new Document()
                        .append("_id", new ObjectId())
                        .append("reportId", firstReportId)
                        .append("imageUrl", "http://example.com/img" + i + ".jpg")
                        .append("uploadDate", LocalDateTime.now().minusHours(i))
        ).toList();
        // Crear 5 comentarios para el primer reporte
        List<Comment> comments = IntStream.rangeClosed(1, 5).mapToObj(i -> {
            Comment c = new Comment();
            c.setId(new ObjectId());
            c.setReportId(firstReportId);
            c.setUserId(new ObjectId());
            c.setUserName("User" + i);
            c.setComment("Comentario " + i);
            c.setCreatedAt(LocalDateTime.now().minusHours(i));
            c.setCommentStatus(CommentStatus.PUBLISHED);
            return c;
        }).toList();

        mongoTemplate.insertAll(comments);
        var result = mongoTemplate.findAll(Comment.class);
        result.forEach(System.out::println);
        mongoTemplate.getCollection("images").insertMany(images);
        reportRepository.saveAll(existing);

        // Stub: seguridad retorna siempre mismo usuario
        when(securityUtils.getCurrentUserId()).thenReturn(existing.get(0).getUserId());
        when(securityUtils.getCurrentUsername()).thenReturn(existing.get(0).getUserEmail());

    }

    @Test
    @DisplayName("createReport - Flujo exitoso: guarda y retorna ReportResponse")
    void createReport_Success() {
        var req = new ReportRequest(
                "NewTitle", "NewDesc",
                List.of(new CategoryRef("catX")),
                4.5, -75.5
        );

        ReportResponse resp = reportService.createReport(req);

        assertNotNull(resp.id());
        assertEquals("NewTitle", resp.title());
        assertEquals("NewDesc", resp.description());
        assertEquals("PENDING", resp.reportStatus());
        // Verificar que se haya guardado uno más
        assertEquals(existing.size() + 1, reportRepository.count());
    }

    @Test
    @DisplayName("createReport - Debe lanzar DuplicateReportException si ya existe")
    void createReport_Duplicate() {
        // Usa título y descripción de uno existente
        var dup = existing.get(1);
        var req = new ReportRequest(
                dup.getTitle(), dup.getDescription(),
                dup.getCategoryList(),
                dup.getLocation().getY(), dup.getLocation().getX()
        );

        assertThrows(DuplicateReportException.class, () ->
                reportService.createReport(req));

        // No cambia el conteo
        assertEquals(existing.size(), reportRepository.count());
    }

    @Test
    @DisplayName("getReportById - Flujo exitoso: retorna ReportResponse existente")
    void getReportById_Success() {
        var id = existing.get(2).getId().toHexString();
        var resp = reportService.getReportById(id);

        assertEquals(id, resp.id());
        assertEquals(existing.get(2).getTitle(), resp.title());
    }

    @Test
    @DisplayName("getReportById - Debe lanzar ReportNotFoundException si no existe")
    void getReportById_NotFound() {
        String randomId = new ObjectId().toHexString();
        assertThrows(ReportNotFoundException.class, () ->
                reportService.getReportById(randomId));
    }

    @Test
    @DisplayName("getReportById - Debe lanzar IllegalArgumentException si id inválido")
    void getReportById_InvalidId() {
        assertThrows(IdInvalidException.class, () ->
                reportService.getReportById("bad-id"));
    }

    // ------------------ getReportsNearLocation (/paginated) ------------------

    @Test
    @DisplayName("getReportsNearLocation - devuelve paginación por defecto sin categorías")
    void getReportsNearLocation_DefaultPagination_NoCategories() {
        // Arrange
        double lat = 10, lon = 10;

        // Act
        PaginatedReportResponse resp = reportService.getReportsNearLocation(lat, lon, 10d, null, null, null);
        // Assert
        assertEquals(1, resp.page());
        assertEquals(30, resp.size());
        assertEquals(existing.size(), resp.totalElements());
        assertEquals(1, resp.totalPages());
    }

    @Test
    @DisplayName("getReportsNearLocation - aplica filtro de categorías y paginación personalizada")
    void getReportsNearLocation_WithCategories_CustomPage() {
        // Arrange
        double lat = 10, lon = 10;
        List<String> cats = List.of("cat1", "cat2");

        // Act
        PaginatedReportResponse resp = reportService.getReportsNearLocation(lat, lon, 15.0, 2, 1, cats);

        // Assert
        assertEquals(2, resp.page());
        assertEquals(1, resp.size());
        assertEquals(2, resp.totalElements());
        assertEquals(2, resp.totalPages());
    }


    @Test
    @DisplayName("getReportsNearLocation - lanza IllegalArgumentException si coordenadas inválidas")
    void getReportsNearLocation_InvalidCoordinates() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> reportService.getReportsNearLocation(91.0, 0.0, null, null, null, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> reportService.getReportsNearLocation(0.0, 181.0, null, null, null, null))
        );
    }

    @Test
    @DisplayName("getReportsNearLocation - devuelve lista vacía si página excede total")
    void getReportsNearLocation_PageExceedsTotal() {
        // Arrange
        double lat = 10, lon = 10;

        // Act
        PaginatedReportResponse resp = reportService.getReportsNearLocation(lat, lon, null, 11, 5, null);

        // Assert
        assertTrue(resp.content().isEmpty());
        assertEquals(11, resp.page());
        assertEquals(5, resp.size());
        assertEquals(existing.size(), resp.totalElements());
    }


    // ------------------ softDeleteReport (soft delete) ------------------


    @Test
    @DisplayName("softDeleteReport - integración marca como DELETED y crea historial")
    void softDeleteReportIntegration_Success() {
        // Arrange
        Report toDelete = reportRepository.findAll().get(0);
        String id = toDelete.getId().toHexString();

        // Act
        reportService.softDeleteReport(id);

        // Assert: estado modificado en la base de datos
        Report reloaded = reportRepository.findById(new ObjectId(id)).orElseThrow();
        assertEquals(ReportStatus.DELETED, reloaded.getReportStatus());

        // Assert: se creó un historial en Mongo
        List<ReportStatusHistory> hist = historyRepository
                .findByReportId(new ObjectId(id), PageRequest.of(0, 10))
                .getContent();
        assertEquals(1, hist.size());
        assertEquals(ReportStatus.DELETED, hist.get(0).getNewStatus());
    }

    @Test
    @DisplayName("softDeleteReport - integración lanza ReportNotFoundException si no existe")
    void softDeleteReportIntegration_NotFound() {
        String fakeId = new ObjectId().toHexString();
        assertThrows(ReportNotFoundException.class,
                () -> reportService.softDeleteReport(fakeId));
    }

    @Test
    @DisplayName("softDeleteReport - lanza IdInvalidException si id inválido")
    void softDeleteReport_InvalidId() {
        assertThrows(IdInvalidException.class,
                () -> reportService.softDeleteReport("bad-id"));
    }

    // ------------------ getAllImagesByReport (integración) ------------------

    @Test
    @DisplayName("getAllImagesByReport - integración devuelve todas las imágenes para un reporte existente")
    void getAllImagesByReport_Success() {
        // Arrange
        String reportId = existing.get(0).getId().toHexString();

        // Act
        List<ImageResponse> images = reportService.getAllImagesByReport(reportId);

        // Assert
        assertEquals(5, images.size(), "Debe devolver las 5 imágenes insertadas");
        assertTrue(images.stream().allMatch(img -> img.id() != null && img.imageUrl().startsWith("http")));
    }

    @Test
    @DisplayName("getAllImagesByReport - integración lanza ReportNotFoundException si no existe el reporte")
    void getAllImagesByReport_NotFound() {
        String fakeId = new ObjectId().toHexString();
        assertThrows(ReportNotFoundException.class,
                () -> reportService.getAllImagesByReport(fakeId));
    }

    @Test
    @DisplayName("getAllImagesByReport - integración lanza IllegalArgumentException si id inválido")
    void getAllImagesByReport_InvalidId() {
        assertThrows(IdInvalidException.class,
                () -> reportService.getAllImagesByReport("invalid-object-id"));
    }

// ------------------ updateReport (integración) ------------------

    @Test
    @DisplayName("updateReport - integración actualiza campos permitidos correctamente")
    void updateReport_Success() {
        // Arrange
        Report original = reportRepository.findAll().get(1);
        String id = original.getId().toHexString();
        ReportUpdateDto req = new ReportUpdateDto(
                "NuevoTítulo", "NuevaDescripción",
                List.of(new CategoryRef("catX"))
        );

        // Act
        ReportResponse resp = reportService.updateReport(id, req);

        // Assert: respuesta actualizada
        assertEquals(id, resp.id());
        assertEquals("NuevoTítulo", resp.title());
        assertEquals("NuevaDescripción", resp.description());

        // Assert: persistido en la base de datos
        Report reloaded = reportRepository.findById(new ObjectId(id)).orElseThrow();
        assertEquals("NuevoTítulo", reloaded.getTitle());
        assertEquals("NuevaDescripción", reloaded.getDescription());
    }

    @Test
    @DisplayName("updateReport - integración lanza ReportNotFoundException si no existe")
    void updateReport_NotFound() {
        String fakeId = new ObjectId().toHexString();
        var req = new ReportUpdateDto("T", "D", List.of(new CategoryRef("c")));
        assertThrows(ReportNotFoundException.class,
                () -> reportService.updateReport(fakeId, req));
    }

    @Test
    @DisplayName("updateReport - integración lanza IllegalArgumentException si id inválido")
    void updateReport_InvalidId() {
        var req = new ReportUpdateDto("T", "D", List.of(new CategoryRef("c")));
        assertThrows(IdInvalidException.class,
                () -> reportService.updateReport("bad-id", req));
    }


    @Test
    @DisplayName("toggleReportVote - agrega voto si usuario no ha votado antes")
    void toggleReportVote_AddVote_Success() {
        // Arrange
        Report report = existing.get(1);
        String reportId = report.getId().toHexString();
        String userId = new ObjectId().toHexString(); // usuario nuevo

        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        // Act
        reportService.toggleReportVote(reportId);

        // Assert
        Report updated = reportRepository.findById(report.getId()).orElseThrow();
        assertEquals(report.getImportantVotes() + 1, updated.getImportantVotes());
        assertTrue(updated.getLikedUserIds().contains(new ObjectId(userId)));
    }

    @Test
    @DisplayName("toggleReportVote - elimina voto si usuario ya votó")
    void toggleReportVote_RemoveVote_Success() {
        // Arrange
        Report report = existing.get(2);
        ObjectId userId = new ObjectId();
        report.getLikedUserIds().add(userId);
        report.setImportantVotes(report.getImportantVotes() + 1);
        reportRepository.save(report);

        when(securityUtils.getCurrentUserId()).thenReturn(userId.toHexString());

        // Act
        reportService.toggleReportVote(report.getId().toHexString());

        // Assert
        Report updated = reportRepository.findById(report.getId()).orElseThrow();
        assertFalse(updated.getLikedUserIds().contains(userId));
        assertEquals(report.getImportantVotes() - 1, updated.getImportantVotes());
    }

    @Test
    @DisplayName("toggleReportVote - lanza excepción si ID inválido")
    void toggleReportVote_InvalidId_Throws() {
        assertThrows(IdInvalidException.class,
                () -> reportService.toggleReportVote("bad-id"));
    }

    @Test
    @DisplayName("updateReportStatus - actualiza correctamente el estado si usuario es admin")
    void updateReportStatus_Admin_Success() {
        // Arrange
        Report report = existing.get(0);
        String reportId = report.getId().toHexString();

        when(securityUtils.getCurrentUserId()).thenReturn(report.getUserId());
        when(securityUtils.hasRole("ROLE_ADMIN")).thenReturn(true);

        ReportStatusUpdate dto = new ReportStatusUpdate("resolved", null);

        // Act
        reportService.updateReportStatus(reportId, dto);

        // Assert
        Report updated = reportRepository.findById(report.getId()).orElseThrow();
        assertEquals(ReportStatus.RESOLVED, updated.getReportStatus());
    }

    @Test
    @DisplayName("updateReportStatus - lanza excepción si usuario no es admin y no es creador")
    void updateReportStatus_NotOwnerOrAdmin_Throws() {
        // Arrange
        Report report = existing.get(0);
        String reportId = report.getId().toHexString();

        when(securityUtils.getCurrentUserId()).thenReturn(new ObjectId().toHexString());
        when(securityUtils.hasRole("ROLE_ADMIN")).thenReturn(false);

        ReportStatusUpdate dto = new ReportStatusUpdate("resolved", null);

        // Act & Assert
        assertThrows(SecurityException.class,
                () -> reportService.updateReportStatus(reportId, dto));
    }

    @Test
    @DisplayName("updateReportStatus - lanza IllegalArgumentException si ID inválido")
    void updateReportStatus_InvalidId_Throws() {
        ReportStatusUpdate dto = new ReportStatusUpdate("verified", null);
        assertThrows(IdInvalidException.class,
                () -> reportService.updateReportStatus("invalid-id", dto));
    }

    @Test
    @DisplayName("updateReportStatus - usuario es dueño pero no admin, intenta marcar como VERIFIED y falla")
    void updateReportStatus_OwnerButNotAdmin_Verified_Fails() {
        // Arrange
        Report report = existing.get(0); // El dueño
        String reportId = report.getId().toHexString();

        when(securityUtils.getCurrentUserId()).thenReturn(report.getUserId());
        when(securityUtils.hasRole("ROLE_ADMIN")).thenReturn(false);

        ReportStatusUpdate dto = new ReportStatusUpdate("verified", null);

        // Act & Assert
        assertThrows(SecurityException.class,
                () -> reportService.updateReportStatus(reportId, dto));
    }

    @Test
    @DisplayName("updateReportStatus - usuario es dueño pero no admin, intenta marcar como REJECTED y falla")
    void updateReportStatus_OwnerButNotAdmin_Rejected_Fails() {
        // Arrange
        Report report = existing.get(1); // El dueño
        String reportId = report.getId().toHexString();

        when(securityUtils.getCurrentUserId()).thenReturn(report.getUserId());
        when(securityUtils.hasRole("ROLE_ADMIN")).thenReturn(false);

        ReportStatusUpdate dto = new ReportStatusUpdate("rejected", "no me gusta");

        // Act & Assert
        assertThrows(SecurityException.class,
                () -> reportService.updateReportStatus(reportId, dto));
    }

    @Test
    @DisplayName("updateReportStatus - admin rechaza sin justificación y falla")
    void updateReportStatus_AdminRejectedWithoutReason_Fails() {
        // Arrange
        Report report = existing.get(2);
        String reportId = report.getId().toHexString();

        when(securityUtils.getCurrentUserId()).thenReturn("admin-id");
        when(securityUtils.hasRole("ROLE_ADMIN")).thenReturn(true);

        ReportStatusUpdate dto = new ReportStatusUpdate("rejected", "");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> reportService.updateReportStatus(reportId, dto));
    }

    @Test
    @DisplayName("getCommentsByReportId - devuelve comentarios paginados correctamente")
    void getCommentsByReportId_ReturnsPaginatedComments() {
        // Arrange
        String reportId = existing.get(0).getId().toHexString();

        // Act
        CommentPaginatedResponse response = reportService.getCommentsByReportId(reportId, 1, 10);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals(5, response.content().size());
        assertEquals(1, response.page());
        assertEquals(10, response.size());

        response.content().forEach(comment -> {
            assertEquals(reportId, comment.reportId());
            assertNotNull(comment.comment());
            assertNotNull(comment.createdAt());
        });
    }

    @Test
    @DisplayName("getCommentsByReportId - lanza ReportNotFoundException si el reporte no existe")
    void getCommentsByReportId_ReportNotFound() {
        // Arrange
        String fakeId = new ObjectId().toHexString();

        // Act & Assert
        assertThrows(ReportNotFoundException.class,
                () -> reportService.getCommentsByReportId(fakeId, 1, 10));
    }

}

