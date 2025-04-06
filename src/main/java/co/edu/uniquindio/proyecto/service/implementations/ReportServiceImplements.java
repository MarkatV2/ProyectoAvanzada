package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.comment.CommentPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.report.PaginatedReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportRequest;
import co.edu.uniquindio.proyecto.dto.report.ReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportStatusUpdate;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.exception.report.DuplicateReportException;
import co.edu.uniquindio.proyecto.exception.global.IdInvalidException;
import co.edu.uniquindio.proyecto.exception.report.ReportNotFoundException;
import co.edu.uniquindio.proyecto.repository.ReportRepository;
import co.edu.uniquindio.proyecto.service.interfaces.ReportService;
import co.edu.uniquindio.proyecto.service.mapper.ReportMapper;
import co.edu.uniquindio.proyecto.util.SecurityUtils;
import co.edu.uniquindio.proyecto.validator.ReportStatusChangeRequestValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Servicio actualizado
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImplements implements ReportService {

    private final ReportRepository reportRepository;
    private final ImageServiceImplements imageService;
    private final ReportMapper reportMapper;
    private final SecurityUtils securityUtils;
    private final ReportStatusChangeRequestValidator validator;
    private final CommentServiceImplements commentService;

    @Transactional
    public ReportResponse createReport(ReportRequest request){
        if (reportRepository.existsByTitleAndDescription(request.title(), request.description())) { //Que sea del mismo Usuario
            log.warn("Intento de crear un reporte duplicado: {}", request.title());
            throw new DuplicateReportException("El reporte'" + request.title() + "' ya existe");
        }

        Report report = reportMapper.toEntity(request);// NO ESTOY GUARDANDO EL USER ID

        Report savedReport = reportRepository.save(report);
        log.info("Reporte creado con ID: {}", savedReport.getId());
        return reportMapper.toResponse(savedReport);
    }


    public ReportResponse getReportById(String id) {
        if (!ObjectId.isValid(id)) {
            log.warn("Intento de obtener reporte con ID inválido: {}", id);
            throw new ReportNotFoundException(id);
        }
        return reportRepository.findById(new ObjectId(id))
                .map(report -> {
                    log.debug("Reporte encontrado: {}", id);
                    return reportMapper.toResponse(report);
                })
                .orElseThrow(() -> {
                    log.warn("Reporte no encontrado: {}", id);
                    return new ReportNotFoundException(id);
                });
    }

    public PaginatedReportResponse getReportsNearLocation(double latitude, double longitude, Double radiusKm,
            Integer page, Integer size
    ) {
        validateCoordinates(latitude, longitude);

        final double finalRadius = radiusKm != null ? radiusKm : 10;
        final int pageSize = size != null ? Math.min(size, 100) : 30;
        final int pageNumber = page != null ? Math.max(page, 1) : 1;

        GeoJsonPoint location = new GeoJsonPoint(longitude, latitude); // MapBox usa long,lat
        double radiusMeters = finalRadius * 1000;

        // Usamos PageRequest para crear un Pageable
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);

        Page<Report> reportsPage = reportRepository.findNearbyReports(
                location,
                radiusMeters,
                pageable
        );


        return mapToPaginatedResponse(reportsPage, pageNumber);
    }


    @Transactional
    public void softDeleteReport(String reportId) {
        Report report = reportRepository.findById(new ObjectId(reportId))
                .orElseThrow(() -> {
                    log.warn("Reporte no encontrado: {}", reportId);
                    return new ReportNotFoundException(reportId);
                });

        report.setReportStatus(ReportStatus.DELETED);
        reportRepository.save(report);
        log.info("Reporte {} eliminado", reportId);
    }

    public List<ImageResponse> getAllImagesByReport (String reportId){
        log.info("Obteniendo todas las imagenes con el id: {}", reportId);
        ObjectId objectId = parseObjectId(reportId);
        reportRepository.findById(objectId).orElseThrow(() -> new ReportNotFoundException(reportId));
        return imageService.getAllImagesByReport(objectId);
    }


    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitud fuera de rango (-90 a 90)");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitud fuera de rango (-180 a 180)");
        }
    }

    private PaginatedReportResponse mapToPaginatedResponse(Page<Report> page, int currentPage) {
        List<ReportResponse> content = page.getContent()
                .stream()
                .map(reportMapper::toResponse)
                .toList();

        return new PaginatedReportResponse(
                content,
                currentPage,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private ObjectId parseObjectId(String id) {
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException e) {
            log.error("ID inválido: {}", id);
            throw new IdInvalidException("ID no válido");
        }
    }

    @Transactional
    public ReportResponse updateReport(ObjectId reportId, ReportRequest request) {
        Report existing = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Reporte no encontrado con ID: " + reportId));


        // Actualiza solo campos permitidos
        reportMapper.updateEntity(existing, request);
        Report updated = reportRepository.save(existing);
        log.info("Reporte actualizado: {}", updated.getId()); // seria bueno conocer el usuario que actualiza

        return reportMapper.toResponse(updated);
    }


    /**
     * Actualiza el estado de un reporte si el usuario tiene permisos y la transición es válida.
     *
     * @param reportId ID del reporte.
     * @param dto Datos con el nuevo estado.
     * @throws IllegalArgumentException si la transición es inválida o no autorizada.
     */
    public void updateReportStatus(ObjectId reportId, ReportStatusUpdate dto) {
        log.info("Iniciando actualización de estado del reporte: {}", reportId);

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Reporte no encontrado"));

        String currentUserId = securityUtils.getCurrentUserId();
        boolean isAdmin = securityUtils.hasRole("ROLE_ADMIN");

        ReportStatus newStatus = ReportStatus.valueOf(dto.status().toUpperCase());

        // Validar transición
        validator.validate(report, newStatus, dto.rejectionMessage(), isAdmin, currentUserId);

        report.setReportStatus(newStatus);
        log.info("Estado del reporte actualizado a: {}", newStatus);

        if (newStatus == ReportStatus.REJECTED) {
            log.warn("Reporte {} rechazado por el administrador. Motivo: {}", reportId, dto.rejectionMessage());
            // Aquí podrías guardar el mensaje en otro campo o en otra colección si lo deseas
        }

        reportRepository.save(report);
        log.info("Reporte {} guardado con el nuevo estado", reportId);
    }

    /**
     * Alterna el voto (like) de un usuario sobre un reporte. Si el usuario ya votó, se elimina su voto;
     * de lo contrario, se agrega su voto.
     *
     * @param reportId      Identificador del reporte a votar.
     * @throws ReportNotFoundException Si el reporte no se encuentra.
     */
    @Transactional
    public void toggleReportVote(String reportId) {
        log.info("Iniciando toggle de voto para reporte {}", reportId);

        Report report = reportRepository.findById(new ObjectId(reportId))
                .orElseThrow(() -> {
                    log.error("Reporte no encontrado con ID: {}", reportId);
                    return new ReportNotFoundException("Reporte no encontrado");
                });

        String currentUserId = securityUtils.getCurrentUserId();

        ObjectId userObjectId = new ObjectId(currentUserId);
        Set<ObjectId> likedUserIds = report.getLikedUserIds();
        if (likedUserIds == null) {
            likedUserIds = new HashSet<>();
            report.setLikedUserIds(likedUserIds);
        }

        if (likedUserIds.contains(userObjectId)) {
            // El usuario ya ha votado: quitar su voto
            likedUserIds.remove(userObjectId);
            report.setImportantVotes(report.getImportantVotes() - 1);
            log.info("Se ha quitado el voto del usuario {} para el reporte {}", currentUserId, reportId);
        } else {
            // El usuario no ha votado: agregar su voto
            likedUserIds.add(userObjectId);
            report.setImportantVotes(report.getImportantVotes() + 1);
            log.info("Se ha sumado un voto del usuario {} para el reporte {}", currentUserId, reportId);
        }

        reportRepository.save(report);
        log.info("Reporte {} actualizado. Votos importantes: {}", reportId, report.getImportantVotes());
    }

    /**
     * Obtiene los comentarios de un reporte de forma paginada, validando primero la existencia del reporte.
     *
     * @param reportId Identificador del reporte.
     * @param page     Número de página.
     * @param size     Tamaño de página.
     * @return CommentPaginatedResponse con los comentarios.
     * @throws ReportNotFoundException Si el reporte no se encuentra.
     */
    public CommentPaginatedResponse getCommentsByReportId(String reportId, int page, int size) {
        log.info("Validando existencia del reporte con ID: {}", reportId);
        reportRepository.findById(new ObjectId(reportId))
                .orElseThrow(() -> {
                    log.error("No se encontró el reporte con ID: {}", reportId);
                    return new ReportNotFoundException("No se encontró el reporte con ID: " + reportId);
                });

        return commentService.getCommentsByReportId(reportId, page, size);
    }
}
