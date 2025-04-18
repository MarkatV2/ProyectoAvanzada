package org.example.proyectoavanzada.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.*;

import co.edu.uniquindio.proyecto.dto.comment.CommentPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.comment.CommentResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.report.PaginatedReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportRequest;
import co.edu.uniquindio.proyecto.dto.report.ReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusUpdate;
import co.edu.uniquindio.proyecto.entity.category.CategoryRef;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.exception.global.IdInvalidException;
import co.edu.uniquindio.proyecto.exception.report.DuplicateReportException;
import co.edu.uniquindio.proyecto.exception.report.ReportNotFoundException;
import co.edu.uniquindio.proyecto.repository.ReportRepository;
import co.edu.uniquindio.proyecto.service.implementations.NearbyNotificationService;
import co.edu.uniquindio.proyecto.service.implementations.ReportServiceImpl;
import co.edu.uniquindio.proyecto.service.implementations.ReportStatusHistoryServiceImpl;
import co.edu.uniquindio.proyecto.service.interfaces.CommentService;
import co.edu.uniquindio.proyecto.service.interfaces.ImageService;
import co.edu.uniquindio.proyecto.service.mapper.ReportMapper;
import co.edu.uniquindio.proyecto.util.SecurityUtils;
import co.edu.uniquindio.proyecto.validator.ReportStatusChangeRequestValidator;
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
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ReportServiceUnitTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private NearbyNotificationService nearbyNotificationService;

    @Mock
    private ImageService imageService;

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ReportStatusChangeRequestValidator validator;

    @Mock
    private CommentService commentService;

    @Mock
    private ReportStatusHistoryServiceImpl reportStatusHistoryService;

    @InjectMocks
    private ReportServiceImpl reportService;

    // Datos de entrada para createReport
    private ReportRequest reportRequest;
    private ReportResponse reportResponse;
    private final String currentUserId = "user123";
    private final String currentUsername = "usuario123";

    // Lista de reportes preexistentes (al menos 5) simulados en "la base de datos"
    private List<Report> preexistingReports;

    @BeforeEach
    void setUp() {
        // Configurar ReportRequest para crear un nuevo reporte
        reportRequest = new ReportRequest(
                "Incidente en parque",
                "Se reporta un incidente en el parque central",
                List.of(new CategoryRef("Emergencia")),
                10.0,
                10.0
        );

        // Inicializar una lista con 5 reportes precreados en la "BD"
        preexistingReports = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Report report = new Report();
            report.setId(new ObjectId());
            report.setTitle("Reporte " + i);
            report.setDescription("Descripción del reporte " + i);
            report.setCategoryList(List.of(new CategoryRef("Categoria" + i)));
            report.setLocation(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(10.0 + i * 0.001, 10.0 + i * 0.001));
            report.setReportStatus(ReportStatus.PENDING);
            report.setUserEmail("user" + i + "@example.com");
            preexistingReports.add(report);
        }

        // Stub: Configurar la seguridad para que retorne valores fijos (marcados como lenient)
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        lenient().when(securityUtils.getCurrentUsername()).thenReturn(currentUsername);

        // Configurar el ReportResponse que se espera al crear un reporte nuevo
        Report newReportEntity = new Report();
        newReportEntity.setId(new ObjectId());
        newReportEntity.setTitle(reportRequest.title());
        newReportEntity.setDescription(reportRequest.description());
        newReportEntity.setCategoryList(reportRequest.categoryList());
        newReportEntity.setLocation(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(reportRequest.longitude(), reportRequest.latitude()));
        newReportEntity.setReportStatus(ReportStatus.PENDING);
        newReportEntity.setUserEmail(currentUsername);

        reportResponse = new ReportResponse(
                newReportEntity.getId().toHexString(),
                newReportEntity.getTitle(),
                newReportEntity.getDescription(),
                newReportEntity.getCategoryList(),
                ((org.springframework.data.mongodb.core.geo.GeoJsonPoint)newReportEntity.getLocation()).getY(),
                ((org.springframework.data.mongodb.core.geo.GeoJsonPoint)newReportEntity.getLocation()).getX(),
                newReportEntity.getReportStatus().toString(),
                LocalDateTime.now(),
                0,
                currentUserId
        );
    }


    @Test
    @DisplayName("createReport: Flujo exitoso con creación y persistencia")
    void testCreateReportSuccess() {
        // Arrange
        // Simulamos que NO existe reporte duplicado (aunque en la BD hay 5 reportes distintos)
        when(reportRepository.existsByTitleAndDescription(reportRequest.title(), reportRequest.description()))
                .thenReturn(false);

        // Crear el reporte a persistir (nueva entidad a partir de reportRequest)
        Report newReport = getReport();

        when(reportMapper.toEntity(reportRequest, currentUserId, currentUsername)).thenReturn(newReport);
        when(reportRepository.save(newReport)).thenReturn(newReport);
        when(reportMapper.toResponse(newReport)).thenReturn(reportResponse);

        // Act
        ReportResponse response = reportService.createReport(reportRequest);

        // Assert
        assertNotNull(response, "La respuesta no debe ser nula");
        assertEquals(reportResponse.id(), response.id(), "El ID del reporte debe coincidir");
        // Verificar que se haya llamado al servicio de notificación
        verify(nearbyNotificationService, times(1)).notifyUsersNearby(newReport);
        verify(reportRepository, times(1)).existsByTitleAndDescription(reportRequest.title(), reportRequest.description());
        verify(reportMapper, times(1)).toEntity(reportRequest, currentUserId, currentUsername);
        verify(reportRepository, times(1)).save(newReport);
        verify(reportMapper, times(1)).toResponse(newReport);
    }

    private Report getReport() {
        Report newReport = new Report();
        newReport.setId(new ObjectId());
        newReport.setTitle(reportRequest.title());
        newReport.setDescription(reportRequest.description());
        newReport.setCategoryList(reportRequest.categoryList());
        newReport.setLocation(new org.springframework.data.mongodb.core.geo.GeoJsonPoint(reportRequest.longitude(), reportRequest.latitude()));
        newReport.setReportStatus(ReportStatus.PENDING);
        newReport.setUserEmail(currentUsername);
        return newReport;
    }

    @Test
    @DisplayName("createReport: Fallo al intentar crear reporte duplicado")
    void testCreateReportDuplicate() {
        // Arrange: Se simula que existe un reporte duplicado (por ejemplo, uno de los 5 ya preexistentes tiene el mismo título y descripción)
        when(reportRepository.existsByTitleAndDescription(reportRequest.title(), reportRequest.description()))
                .thenReturn(true);

        // Act & Assert: Se espera que se lance DuplicateReportException
        DuplicateReportException exception = assertThrows(DuplicateReportException.class, () -> {
            reportService.createReport(reportRequest);
        }, "Debe lanzar DuplicateReportException al intentar crear reporte duplicado");
        assertTrue(exception.getMessage().contains(reportRequest.title()), "El mensaje debe contener el título");

        verify(reportRepository, times(1)).existsByTitleAndDescription(reportRequest.title(), reportRequest.description());
        verify(securityUtils, never()).getCurrentUserId();
        verify(reportMapper, never()).toEntity(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("getReportById: Reporte encontrado en base con 5 reportes pre-creados")
    void testGetReportByIdSuccess() {
        // Arrange: Seleccionamos uno de los 5 reportes preexistentes; por ejemplo, el tercero
        Report selectedReport = preexistingReports.get(2);
        String idHex = selectedReport.getId().toHexString();

        when(reportRepository.findById(eq(selectedReport.getId()))).thenReturn(Optional.of(selectedReport));
        when(reportMapper.toResponse(selectedReport)).thenReturn(
                new ReportResponse(
                        selectedReport.getId().toHexString(),
                        selectedReport.getTitle(),
                        selectedReport.getDescription(),
                        selectedReport.getCategoryList(),
                        ((org.springframework.data.mongodb.core.geo.GeoJsonPoint) selectedReport.getLocation()).getY(),
                        ((org.springframework.data.mongodb.core.geo.GeoJsonPoint) selectedReport.getLocation()).getX(),
                        selectedReport.getReportStatus().toString(),
                        LocalDateTime.now(),
                        0,
                        currentUserId
                )
        );

        // Act
        ReportResponse response = reportService.getReportById(idHex);

        // Assert
        assertNotNull(response, "La respuesta no debe ser nula");
        assertEquals(selectedReport.getId().toHexString(), response.id(), "El ID debe coincidir");

        verify(reportRepository, times(1)).findById(eq(selectedReport.getId()));
        verify(reportMapper, times(1)).toResponse(selectedReport);
    }

    @Test
    @DisplayName("getReportById: Reporte no encontrado y se lanza ReportNotFoundException")
    void testGetReportByIdNotFound() {
        // Arrange: Usamos un id inexistente (nuevo ObjectId)
        ObjectId nonExistingId = new ObjectId();
        String idHex = nonExistingId.toHexString();
        when(reportRepository.findById(eq(nonExistingId))).thenReturn(Optional.empty());

        // Act & Assert
        ReportNotFoundException exception = assertThrows(ReportNotFoundException.class, () -> {
            reportService.getReportById(idHex);
        }, "Se debe lanzar ReportNotFoundException al no encontrar el reporte");

        verify(reportRepository, times(1)).findById(eq(nonExistingId));
        verify(reportMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("getReportsNearLocation: éxito sin categorías")
    void testGetReportsWithoutCategories() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 30);
        Page<Report> reportPage = new PageImpl<>(preexistingReports, pageable, preexistingReports.size());

        // Mock del repositorio
        when(reportRepository.findNearbyReports(any(GeoJsonPoint.class), eq(10000.0), any(Pageable.class)))
                .thenReturn(reportPage);

        // Mock del mapper para cada reporte
        when(reportMapper.toResponseList(preexistingReports)).thenReturn(
                preexistingReports.stream().map(report ->
                        new ReportResponse(
                                report.getId().toHexString(),
                                report.getTitle(),
                                report.getDescription(),
                                report.getCategoryList(),
                                ((GeoJsonPoint) report.getLocation()).getY(),
                                ((GeoJsonPoint) report.getLocation()).getX(),
                                report.getReportStatus().name(),
                                LocalDateTime.now(),
                                0,
                                report.getUserEmail()
                        )
                ).toList()
        );

        // Act
        PaginatedReportResponse result = reportService.getReportsNearLocation(10.0, 10.0, null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.content().size(), "Debe retornar 5 reportes");
        assertEquals(1, result.totalPages());
        assertEquals(5, result.totalElements());
        assertEquals(30, result.size());
        assertEquals(1, result.page());

        // Verificación
        verify(reportRepository).findNearbyReports(any(GeoJsonPoint.class), eq(10000.0), any(Pageable.class));
}


    @Test
    @DisplayName("getReportsNearLocation: éxito con filtro de categorías")
    void testGetReportsWithCategories() {
        // Arrange
        List<String> categories = List.of("Categoria2");
        PageRequest pageable = PageRequest.of(0, 10);
        Report matchingReport = preexistingReports.get(1); // Tiene "Categoria2"
        Page<Report> reportPage = new PageImpl<>(List.of(matchingReport), pageable, 1);

        when(reportRepository.findNearbyReportsByCategoryNames(any(GeoJsonPoint.class), eq(5000.0), eq(categories), any(Pageable.class)))
                .thenReturn(reportPage);

        when(reportMapper.toResponseList(List.of(matchingReport))).thenReturn(
                List.of(new ReportResponse(
                        matchingReport.getId().toHexString(),
                        matchingReport.getTitle(),
                        matchingReport.getDescription(),
                        matchingReport.getCategoryList(),
                        ((GeoJsonPoint) matchingReport.getLocation()).getY(),
                        ((GeoJsonPoint) matchingReport.getLocation()).getX(),
                        matchingReport.getReportStatus().name(),
                        LocalDateTime.now(),
                        0,
                        matchingReport.getUserEmail()
                ))
        );

        // Act
        PaginatedReportResponse result = reportService.getReportsNearLocation(10.0, 10.0, 5.0, 1, 10, categories);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("Reporte 2", result.content().get(0).title());
        assertEquals(1, result.page());
        assertEquals(1, result.totalPages());
        assertEquals(1, result.totalElements());

        verify(reportRepository).findNearbyReportsByCategoryNames(any(GeoJsonPoint.class), eq(5000.0), eq(categories), any(Pageable.class));
        verify(reportMapper).toResponseList(List.of(matchingReport));
    }


    @Test
    @DisplayName("getReportsNearLocation: usa tamaño máximo permitido (size > 100)")
    void testPageSizeGreaterThanMax() {
        // Arrange
        GeoJsonPoint point = new GeoJsonPoint(10.0, 10.0);
        PageRequest pageable = PageRequest.of(0, 100);
        Page<Report> reportPage = new PageImpl<>(List.of(), pageable, 0);

        when(reportRepository.findNearbyReports(eq(point), eq(1000.0), eq(pageable)))
                .thenReturn(reportPage);

        // Act
        PaginatedReportResponse result = reportService.getReportsNearLocation(10.0, 10.0, 1.0, 1, 999, null);

        // Assert
        assertEquals(100, result.size());
        assertEquals(1, result.page());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());

        verify(reportRepository).findNearbyReports(eq(point), eq(1000.0), eq(pageable));
    }

    @Test
    @DisplayName("getReportsNearLocation: lanza excepción por coordenadas inválidas")
    void testInvalidCoordinates() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            reportService.getReportsNearLocation(-91.0, 181.0, null, null, null, null);
        });
    }

    @Test
    @DisplayName("getReportsNearLocation: usa página mínima (page < 1)")
    void testPageLessThanOne() {
        // Arrange
        GeoJsonPoint point = new GeoJsonPoint(10.0, 10.0);
        PageRequest pageable = PageRequest.of(0, 30);
        Page<Report> reportPage = new PageImpl<>(List.of(), pageable, 0);

        when(reportRepository.findNearbyReports(eq(point), eq(10000.0), eq(pageable)))
                .thenReturn(reportPage);

        // Act
        PaginatedReportResponse result = reportService.getReportsNearLocation(10.0, 10.0, null, 0, null, null);

        // Assert
        assertEquals(1, result.page());
        verify(reportRepository).findNearbyReports(eq(point), eq(10000.0), eq(pageable));
    }

    // ========================================================================
    // PRUEBAS PARA softDeleteReport
    // ========================================================================

    @Test
    @DisplayName("softDeleteReport - Debe cambiar estado a DELETED y guardar reporte cuando existe")
    void softDeleteReport_ShouldChangeStatusToDeletedAndSave_WhenReportExists() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        String userId = new ObjectId().toHexString();
        Report report = new Report();
        report.setId(new ObjectId(reportId));
        report.setReportStatus(ReportStatus.PENDING);

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(report));
        when(reportRepository.save(any(Report.class))).thenReturn(report);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        // Act
        reportService.softDeleteReport(reportId);

        // Assert
        verify(reportStatusHistoryService).createHistory(
                eq(report.getId()),
                eq(new ObjectId(userId)),
                eq(ReportStatus.PENDING),
                eq(ReportStatus.DELETED)
        );
        verify(reportRepository).save(report);
        assertEquals(ReportStatus.DELETED, report.getReportStatus());
    }

    @Test
    @DisplayName("softDeleteReport - Debe lanzar ReportNotFoundException cuando el reporte no existe")
    void softDeleteReport_ShouldThrowReportNotFoundException_WhenReportDoesNotExist() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ReportNotFoundException.class, () -> {
            reportService.softDeleteReport(reportId);
        });

        verify(reportRepository).findById(new ObjectId(reportId));
        verify(reportRepository, never()).save(any());
    }

    // ========================================================================
    // PRUEBAS PARA getAllImagesByReport
    // ========================================================================

    @Test
    @DisplayName("getAllImagesByReport - Debe retornar lista de imágenes cuando el reporte existe")
    void getAllImagesByReport_ShouldReturnImageList_WhenReportExists() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        Report report = new Report();
        report.setId(new ObjectId(reportId));

        ImageResponse image1 = new ImageResponse("img1", "http://example.com/img1.jpg", LocalDateTime.now());
        ImageResponse image2 = new ImageResponse("img2", "http://example.com/img2.jpg", LocalDateTime.now());
        List<ImageResponse> expectedImages = List.of(image1, image2);

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(report));
        when(imageService.getAllImagesByReport(new ObjectId(reportId))).thenReturn(expectedImages);

        // Act
        List<ImageResponse> result = reportService.getAllImagesByReport(reportId);

        // Assert
        verify(reportRepository).findById(new ObjectId(reportId));
        verify(imageService).getAllImagesByReport(new ObjectId(reportId));
        assertEquals(2, result.size());
        assertEquals(expectedImages, result);
    }

    @Test
    @DisplayName("getAllImagesByReport - Debe lanzar ReportNotFoundException cuando el reporte no existe")
    void getAllImagesByReport_ShouldThrowReportNotFoundException_WhenReportDoesNotExist() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ReportNotFoundException.class, () -> {
            reportService.getAllImagesByReport(reportId);
        });

        verify(reportRepository).findById(new ObjectId(reportId));
        verify(imageService, never()).getAllImagesByReport(any());
    }


    @Test
    @DisplayName("toggleReportVote - Debe agregar voto cuando usuario no ha votado")
    void toggleReportVote_ShouldAddVote_WhenUserHasNotVoted() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        String userId = "507f1f77bcf86cd799439012";
        Report report = new Report();
        report.setId(new ObjectId(reportId));
        report.setImportantVotes(0);
        report.setLikedUserIds(new HashSet<>());

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(report));
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(reportRepository.save(any(Report.class))).thenReturn(report);

        // Act
        reportService.toggleReportVote(reportId);

        // Assert
        verify(reportRepository).save(report);
        assertEquals(1, report.getImportantVotes());
        assertTrue(report.getLikedUserIds().contains(new ObjectId(userId)));
    }

    @Test
    @DisplayName("toggleReportVote - Debe quitar voto cuando usuario ya ha votado")
    void toggleReportVote_ShouldRemoveVote_WhenUserHasAlreadyVoted() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        String userId = "507f1f77bcf86cd799439012";
        ObjectId userObjectId = new ObjectId(userId);

        Report report = new Report();
        report.setId(new ObjectId(reportId));
        report.setImportantVotes(1);
        report.setLikedUserIds(new HashSet<>(Set.of(userObjectId)));

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(report));
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(reportRepository.save(any(Report.class))).thenReturn(report);

        // Act
        reportService.toggleReportVote(reportId);

        // Assert
        verify(reportRepository).save(report);
        assertEquals(0, report.getImportantVotes());
        assertFalse(report.getLikedUserIds().contains(userObjectId));
    }

    @Test
    @DisplayName("toggleReportVote - Debe lanzar ReportNotFoundException cuando reporte no existe")
    void toggleReportVote_ShouldThrowReportNotFoundException_WhenReportDoesNotExist() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ReportNotFoundException.class, () -> {
            reportService.toggleReportVote(reportId);
        });

        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleReportVote - Debe lanzar IdInvalidException cuando ID de usuario es inválido")
    void toggleReportVote_ShouldThrowIdInvalidException_WhenUserIdIsInvalid() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        String invalidUserId = "invalid_id";
        Report report = new Report();
        report.setId(new ObjectId(reportId));

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(report));
        when(securityUtils.getCurrentUserId()).thenReturn(invalidUserId);

        // Act & Assert
        assertThrows(IdInvalidException.class, () -> {
            reportService.toggleReportVote(reportId);
        });

        verify(reportRepository, never()).save(any());
    }


    @Test
    @DisplayName("updateReport - Debe actualizar reporte existente correctamente")
    void updateReport_ShouldUpdateExistingReport_WhenValidData() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        String userId = "507f1f77bcf86cd799439012";
        ReportRequest request = new ReportRequest(
                "Nuevo título",
                "Nueva descripción",
                List.of(new CategoryRef("Nueva categoría")),
                10.5,
                20.5
        );

        Report existingReport = new Report();
        existingReport.setId(new ObjectId(reportId));
        existingReport.setTitle("Título antiguo");

        Report updatedReport = new Report();
        updatedReport.setId(new ObjectId(reportId));
        updatedReport.setTitle(request.title());

        ReportResponse expectedResponse = new ReportResponse(
                reportId,
                request.title(),
                request.description(),
                request.categoryList(),
                request.latitude(),
                request.longitude(),
                "PENDING",
                LocalDateTime.now(),
                0,
                userId
        );

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(existingReport));
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(reportRepository.save(any(Report.class))).thenReturn(updatedReport);
        when(reportMapper.toResponse(updatedReport)).thenReturn(expectedResponse);

        // Act
        ReportResponse result = reportService.updateReport(reportId, request);

        // Assert
        verify(reportMapper).updateEntity(existingReport, request);
        verify(reportRepository).save(existingReport);
        assertEquals(expectedResponse, result);
    }

    @Test
    @DisplayName("updateReport - Debe lanzar ReportNotFoundException cuando reporte no existe")
    void updateReport_ShouldThrowReportNotFoundException_WhenReportNotFound() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        ReportRequest request = new ReportRequest(
                "Título",
                "Descripción",
                List.of(new CategoryRef("Categoría")),
                10.0,
                20.0
        );

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ReportNotFoundException.class, () -> {
            reportService.updateReport(reportId, request);
        });

        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateReport - Debe lanzar IdInvalidException cuando ID es inválido")
    void updateReport_ShouldThrowIdInvalidException_WhenInvalidId() {
        // Arrange
        String invalidReportId = "id_invalido";
        ReportRequest request = new ReportRequest(
                "Título",
                "Descripción",
                List.of(new CategoryRef("Categoría")),
                10.0,
                20.0
        );

        // Act & Assert
        assertThrows(IdInvalidException.class, () -> {
            reportService.updateReport(invalidReportId, request);
        });

        verify(reportRepository, never()).findById(any());
        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateReport - Debe registrar usuario que realiza la actualización")
    void updateReport_ShouldLogUpdatingUser_WhenSuccessful() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        String userId = "507f1f77bcf86cd799439012";
        ReportRequest request = new ReportRequest(
                "Título",
                "Descripción",
                List.of(new CategoryRef("Categoría")),
                10.0,
                20.0
        );

        Report existingReport = new Report();
        existingReport.setId(new ObjectId(reportId));

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(existingReport));
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(reportRepository.save(any(Report.class))).thenReturn(existingReport);

        // Act
        reportService.updateReport(reportId, request);

        // Assert
        verify(securityUtils).getCurrentUserId();
    }


    @Test
    @DisplayName("getCommentsByReportId - Debe retornar comentarios paginados cuando reporte existe")
    void getCommentsByReportId_ShouldReturnPaginatedComments_WhenReportExists() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        String userId = "507f1f77bcf86cd799439012";
        String userName = "testuser@example.com";
        int page = 0;
        int size = 10;

        Report existingReport = new Report();
        existingReport.setId(new ObjectId(reportId));

        CommentResponse commentResponse = new CommentResponse(
                "com1",
                userName,
                userId,
                reportId,
                "Texto del comentario",
                LocalDateTime.now()
        );

        CommentPaginatedResponse expectedResponse = new CommentPaginatedResponse(
                List.of(commentResponse),
                page,
                size,
                1L,
                1
        );

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(existingReport));
        when(commentService.getCommentsByReportId(reportId, page, size)).thenReturn(expectedResponse);

        // Act
        CommentPaginatedResponse result = reportService.getCommentsByReportId(reportId, page, size);

        // Assert
        verify(reportRepository).findById(new ObjectId(reportId));
        verify(commentService).getCommentsByReportId(reportId, page, size);
        assertEquals(expectedResponse.content(), result.content());
        assertEquals(page, result.page());
        assertEquals(size, result.size());
        assertEquals(1L, result.totalElements());
        assertEquals(1, result.totalPages());
    }

    @Test
    @DisplayName("getCommentsByReportId - Debe retornar lista vacía cuando no hay comentarios")
    void getCommentsByReportId_ShouldReturnEmptyList_WhenNoCommentsExist() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        int page = 0;
        int size = 10;

        Report existingReport = new Report();
        existingReport.setId(new ObjectId(reportId));

        CommentPaginatedResponse expectedResponse = new CommentPaginatedResponse(
                Collections.emptyList(),
                page,
                size,
                0L,
                0
        );

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(existingReport));
        when(commentService.getCommentsByReportId(reportId, page, size)).thenReturn(expectedResponse);

        // Act
        CommentPaginatedResponse result = reportService.getCommentsByReportId(reportId, page, size);

        // Assert
        assertTrue(result.content().isEmpty());
        assertEquals(0L, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    @Test
    @DisplayName("getCommentsByReportId - Debe lanzar ReportNotFoundException cuando reporte no existe")
    void getCommentsByReportId_ShouldThrowReportNotFoundException_WhenReportNotFound() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        int page = 0;
        int size = 10;

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ReportNotFoundException.class, () -> {
            reportService.getCommentsByReportId(reportId, page, size);
        });

        verify(commentService, never()).getCommentsByReportId(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("getCommentsByReportId - Debe propagar correctamente los parámetros de paginación")
    void getCommentsByReportId_ShouldPropagatePaginationParameters() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        int page = 2;
        int size = 5;

        Report existingReport = new Report();
        existingReport.setId(new ObjectId(reportId));

        CommentPaginatedResponse expectedResponse = new CommentPaginatedResponse(
                Collections.emptyList(),
                page,
                size,
                0L,
                0
        );

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(existingReport));
        when(commentService.getCommentsByReportId(reportId, page, size)).thenReturn(expectedResponse);

        // Act
        CommentPaginatedResponse result = reportService.getCommentsByReportId(reportId, page, size);

        // Assert
        assertEquals(page, result.page());
        assertEquals(size, result.size());
        verify(commentService).getCommentsByReportId(reportId, page, size);
    }


    @Test
    @DisplayName("updateReportStatus - Debe actualizar estado correctamente (PENDING → VERIFIED)")
    void updateReportStatus_ShouldUpdateStatus_WhenValidTransition() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        String userId = "507f1f77bcf86cd799439012";
        ReportStatusUpdate dto = new ReportStatusUpdate("VERIFIED", null);

        Report report = new Report();
        report.setId(new ObjectId(reportId));
        report.setReportStatus(ReportStatus.PENDING);

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(report));
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.hasRole("ROLE_ADMIN")).thenReturn(true);

        // Act
        reportService.updateReportStatus(reportId, dto);

        // Assert
        assertEquals(ReportStatus.VERIFIED, report.getReportStatus());
        verify(reportRepository).save(report);
    }

    @Test
    @DisplayName("updateReportStatus - Debe permitir PENDING → VERIFIED para admin")
    void updateReportStatus_ShouldAllowPendingToVerified_ForAdmin() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        String userId = "507f1f77bcf86cd799439012";
        ReportStatusUpdate dto = new ReportStatusUpdate("VERIFIED", null);

        Report report = new Report();
        report.setId(new ObjectId(reportId));
        report.setReportStatus(ReportStatus.PENDING);
        report.setUserId(new ObjectId(userId));

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(report));
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.hasRole("ROLE_ADMIN")).thenReturn(true);

        // Act
        reportService.updateReportStatus(reportId, dto);

        // Assert
        assertEquals(ReportStatus.VERIFIED, report.getReportStatus());
        verify(reportRepository).save(report);
    }


    @Test
    @DisplayName("updateReportStatus - Debe permitir PENDING → RESOLVED para creador")
    void updateReportStatus_ShouldAllowPendingToResolved_ForCreator() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        String userId = "507f1f77bcf86cd799439012";
        ReportStatusUpdate dto = new ReportStatusUpdate("RESOLVED", null);

        Report report = new Report();
        report.setId(new ObjectId(reportId));
        report.setReportStatus(ReportStatus.PENDING);
        report.setUserId(new ObjectId(userId));

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(report));
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.hasRole("ROLE_ADMIN")).thenReturn(false);

        // Act
        reportService.updateReportStatus(reportId, dto);

        // Assert
        assertEquals(ReportStatus.RESOLVED, report.getReportStatus());
        verify(reportRepository).save(report);
    }


    @Test
    @DisplayName("updateReportStatus - Debe permitir cualquier transición para admin")
    void updateReportStatus_ShouldAllowAnyTransition_ForAdmin() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        String userId = "507f1f77bcf86cd799439012";
        ReportStatusUpdate dto = new ReportStatusUpdate("RESOLVED", null);

        Report report = new Report();
        report.setId(new ObjectId(reportId));
        report.setReportStatus(ReportStatus.VERIFIED);
        report.setUserId(new ObjectId("507f1f77bcf86cd799439013")); // Diferente al current user

        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(report));
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.hasRole("ROLE_ADMIN")).thenReturn(true);

        // Act
        reportService.updateReportStatus(reportId, dto);

        // Assert
        assertEquals(ReportStatus.RESOLVED, report.getReportStatus());
        verify(reportRepository).save(report);
    }


    @Test
    @DisplayName("updateReportStatus - Debe rechazar PENDING → RESOLVED para no creador")
    void updateReportStatus_ShouldRejectPendingToResolved_ForNonCreator() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        String creatorId = "507f1f77bcf86cd799439012";
        String otherUserId = "507f1f77bcf86cd799439013";
        ReportStatusUpdate dto = new ReportStatusUpdate("RESOLVED", null);

        Report report = new Report();
        report.setId(new ObjectId(reportId));
        report.setReportStatus(ReportStatus.PENDING);
        report.setUserId(new ObjectId(creatorId));

        // Configurar mocks
        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(report));
        when(securityUtils.getCurrentUserId()).thenReturn(otherUserId);
        when(securityUtils.hasRole("ROLE_ADMIN")).thenReturn(false);

        // Mockear el validador para lanzar excepción
        doThrow(new SecurityException("No autorizado para cambiar a RESOLVED"))
                .when(validator)
                .validate(report, ReportStatus.RESOLVED, null, false, otherUserId);

        // Act & Assert
        assertThrows(SecurityException.class, () -> {
            reportService.updateReportStatus(reportId, dto);
        });
        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateReportStatus - Debe requerir mensaje para PENDING → REJECTED")
    void updateReportStatus_ShouldRequireMessage_ForRejection() {
        // Arrange
        String reportId = "507f1f77bcf86cd799439011";
        String userId = "507f1f77bcf86cd799439012";
        ReportStatusUpdate dto = new ReportStatusUpdate("REJECTED", null);

        Report report = new Report();
        report.setId(new ObjectId(reportId));
        report.setReportStatus(ReportStatus.PENDING);
        report.setUserId(new ObjectId(userId));

        // Configurar mocks
        when(reportRepository.findById(new ObjectId(reportId))).thenReturn(Optional.of(report));
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.hasRole("ROLE_ADMIN")).thenReturn(true);

        // Mockear el validador para lanzar excepción
        doThrow(new IllegalArgumentException("Debe proporcionar un mensaje de rechazo"))
                .when(validator)
                .validate(report, ReportStatus.REJECTED, null, true, userId);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            reportService.updateReportStatus(reportId, dto);
        });
        verify(reportRepository, never()).save(any());
    }

}

