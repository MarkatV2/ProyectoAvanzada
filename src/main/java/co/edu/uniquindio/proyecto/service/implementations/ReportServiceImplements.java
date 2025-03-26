package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.report.PaginatedReportResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportRequest;
import co.edu.uniquindio.proyecto.dto.report.ReportResponse;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.exception.DuplicateReportException;
import co.edu.uniquindio.proyecto.exception.IdInvalidException;
import co.edu.uniquindio.proyecto.exception.ReportNotFoundException;
import co.edu.uniquindio.proyecto.repository.ReportRepository;
import co.edu.uniquindio.proyecto.service.interfaces.ReportService;
import co.edu.uniquindio.proyecto.service.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Servicio actualizado
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImplements implements ReportService {

    private final ReportRepository reportRepository;
    private final ImageServiceImplements imageService;
    private final ReportMapper reportMapper;

    @Transactional
    public ReportResponse createReport(ReportRequest request){
        if (reportRepository.existsByTitleAndDescription(request.title(), request.description())) { //Que sea del mismo Usuario
            log.warn("Intento de crear un reporte duplicado: {}", request.title());
            throw new DuplicateReportException("El reporte'" + request.title() + "' ya existe");
        }

        Report report = reportMapper.toEntity(request);

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

}
