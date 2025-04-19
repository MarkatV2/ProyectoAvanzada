package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.comment.CommentPaginatedResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.report.*;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.exception.report.DuplicateReportException;
import co.edu.uniquindio.proyecto.exception.global.IdInvalidException;
import co.edu.uniquindio.proyecto.exception.report.ReportNotFoundException;
import co.edu.uniquindio.proyecto.repository.ReportRepository;
import co.edu.uniquindio.proyecto.service.interfaces.CommentService;
import co.edu.uniquindio.proyecto.service.interfaces.ImageService;
import co.edu.uniquindio.proyecto.service.interfaces.ReportService;
import co.edu.uniquindio.proyecto.service.interfaces.ReportStatusHistoryService;
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

/**
 * Implementación del servicio de gestión de reportes.
 * <p>
 * Esta clase se encarga de la lógica relacionada con:
 * <ul>
 *   <li>Creación y actualización de reportes</li>
 *   <li>Transiciones de estado de reportes con validaciones específicas</li>
 *   <li>Gestión de votos importantes (likes) sobre reportes</li>
 *   <li>Obtención de comentarios paginados asociados a un reporte</li>
 *   <li>Creación de historial de cambios de estado</li>
 * </ul>
 *
 * También se encarga de asegurar las reglas de negocio relacionadas con los usuarios administradores
 * y los creadores del reporte, diferenciando sus permisos al realizar cambios.
 *
 * <p>
 * Esta clase utiliza las siguientes dependencias:
 * <ul>
 *   <li>{@link ReportRepository} para operaciones con la base de datos MongoDB</li>
 *   <li>{@link ReportStatusChangeRequestValidator} para validar reglas de negocio en transiciones de estado</li>
 *   <li>{@link ReportStatusHistoryService} para registrar el historial de estados</li>
 *   <li>{@link SecurityUtils} para obtener información del usuario autenticado</li>
 *   <li>{@link CommentService} para acceder a los comentarios de reportes</li>
 * </ul>
 *
 * Los logs se usan para registrar eventos importantes en la aplicación, como cambios de estado,
 * votos y validaciones fallidas.
 * </p>
 *
 * @see Report
 * @see ReportStatus
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final NearbyNotificationService nearbyNotificationService;
    private final ImageService imageService;
    private final ReportMapper reportMapper;
    private final SecurityUtils securityUtils;
    private final ReportStatusChangeRequestValidator validator;
    private final CommentService commentService;
    private final ReportStatusHistoryService reportStatusHistoryService;


    /**
     * Crea un nuevo reporte en la plataforma. Valida duplicados y asigna información del usuario actual.
     * Notifica a los usuarios cercanos al nuevo incidente.
     *
     * @param request datos del reporte a crear.
     * @return información del reporte creado.
     * @throws DuplicateReportException si ya existe un reporte con mismo título y descripción.
     */
    @Transactional
    @Override
    public ReportResponse createReport(ReportRequest request) {
        if (reportRepository.existsByTitleAndDescription(request.title(), request.description())) {
            log.warn("Intento de crear un reporte duplicado: título='{}'", request.title());
            throw new DuplicateReportException("El reporte '" + request.title() + "' ya existe");
        }

        String currentUserId = securityUtils.getCurrentUserId();
        String currentUsername = securityUtils.getCurrentUsername();
        Report report = reportMapper.toEntity(request, currentUserId, currentUsername);

        Report savedReport = reportRepository.save(report);
        log.info("Reporte creado exitosamente con ID: {}", savedReport.getId());

        nearbyNotificationService.notifyUsersNearby(savedReport);
        return reportMapper.toResponse(savedReport);
    }


    /**
     * Busca un reporte por su ID.
     *
     * @param id identificador del reporte.
     * @return representación del reporte encontrado.
     * @throws ReportNotFoundException si el ID es inválido o no existe en base de datos.
     */
    @Override
    public ReportResponse getReportById(String id) {
        ObjectId reportId = parseObjectId(id);
        return reportMapper.toResponse(findReportById(reportId));
    }


    /**
     * Obtiene reportes cercanos a una ubicación geográfica específica, con paginación y filtro por radio.
     *
     * @param latitude   Latitud de la ubicación central.
     * @param longitude  Longitud de la ubicación central.
     * @param radiusKm   Radio en kilómetros (por defecto 10km si es nulo).
     * @param page       Número de página (por defecto 1).
     * @param size       Tamaño de página (máximo 100, por defecto 30).
     * @param categories
     * @return PaginatedReportResponse con los reportes encontrados cerca de la ubicación especificada.
     * @throws IllegalArgumentException si las coordenadas son inválidas.
     */
    @Override
    public PaginatedReportResponse getReportsNearLocation(double latitude, double longitude, Double radiusKm,
            Integer page, Integer size, List<String> categories) {
        validateCoordinates(latitude, longitude);
        log.info("Buscando reportes cerca de la ubicación [lat: {}, lon: {}] con radio={}km", latitude, longitude, radiusKm);

        final double finalRadiusKm = radiusKm != null ? radiusKm : 10.0;
        final int pageSize = size != null ? Math.min(size, 100) : 30;
        final int pageNumber = page != null ? Math.max(page, 1) : 1;

        double radiusMeters = finalRadiusKm * 1000;
        GeoJsonPoint location = new GeoJsonPoint(longitude, latitude); // GeoJSON usa [lon, lat]

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        Page<Report> reportsPage;
        if (categories != null && !categories.isEmpty()) {
            reportsPage = reportRepository.findNearbyReportsByCategoryNames(location, radiusMeters, categories, pageable);
        } else {
            reportsPage = reportRepository.findNearbyReports(location, radiusMeters, pageable);
        }

        log.info("Se encontraron {} reportes cerca de la ubicación (página {} de {})",
                reportsPage.getNumberOfElements(), pageNumber, reportsPage.getTotalPages());

        return mapToPaginatedResponse(reportsPage, pageNumber);
    }


    /**
     * Realiza un soft delete de un reporte, cambiando su estado a DELETED y registrando el cambio en el historial.
     *
     * @param reportId ID del reporte a eliminar.
     * @throws ReportNotFoundException si no se encuentra el reporte.
     */
    @Transactional
    @Override
    public void softDeleteReport(String reportId) {
        log.info("Iniciando eliminación lógica del reporte con ID: {}", reportId);
        ObjectId reportObjectId = parseObjectId(reportId);
        Report report = findReportById(reportObjectId);

        createHistoryReport(report, ReportStatus.DELETED);
        report.setReportStatus(ReportStatus.DELETED);

        reportRepository.save(report);
        log.info("Reporte con ID {} marcado como DELETED", reportId);
    }


    /**
     * Obtiene todas las imágenes asociadas a un reporte.
     *
     * @param reportId ID del reporte.
     * @return Lista de imágenes asociadas al reporte.
     * @throws ReportNotFoundException si no se encuentra el reporte.
     */
    @Override
    public List<ImageResponse> getAllImagesByReport(String reportId) {
        log.info("Obteniendo todas las imágenes del reporte con ID: {}", reportId);
        ObjectId objectId = parseObjectId(reportId);

        // Validar la existencia del reporte antes de proceder
        findReportById(parseObjectId(reportId));

        List<ImageResponse> images = imageService.getAllImagesByReport(objectId);
        log.info("Se encontraron {} imágenes para el reporte {}", images.size(), reportId);
        return images;
    }


    /**
     * Actualiza un reporte existente con los datos proporcionados.
     * Solo actualiza los campos permitidos y devuelve el reporte actualizado.
     *
     * @param reportId ID del reporte a actualizar.
     * @param request  Nuevos datos para actualizar el reporte.
     * @return ReportResponse con el reporte actualizado.
     * @throws ReportNotFoundException Si no se encuentra el reporte.
     */
    @Transactional
    @Override
    public ReportResponse updateReport(String reportId, ReportUpdateDto request) {

        log.info("Actualizando el reporte con ID: {}", reportId);

        ObjectId reportObjectId = parseObjectId(reportId);
        Report existing = findReportById(reportObjectId);

        // Actualiza solo campos permitidos
        reportMapper.updateEntity(existing, request);

        // Guardamos el reporte actualizado
        Report updated = reportRepository.save(existing);

        // Registrar el usuario que realiza la actualización
        String currentUserId = securityUtils.getCurrentUserId();
        log.info("Reporte actualizado por el usuario con ID: {}. Reporte ID: {}", currentUserId, updated.getId());

        return reportMapper.toResponse(updated);
    }


    /**
     * Actualiza el estado de un reporte si el usuario tiene permisos y la transición es válida.
     *
     * @param reportId id del reporte.
     * @param dto Datos con el nuevo estado.
     * @throws IllegalArgumentException si la transición es inválida o no autorizada.
     */
    @Override
    @Transactional
    public void updateReportStatus(String reportId, ReportStatusUpdate dto) {
        log.info("Iniciando actualización de estado del reporte con ID: {}", reportId);

        // Obtener reporte
        ObjectId reportObjectId = parseObjectId(reportId);
        Report report = findReportById(reportObjectId);

        String currentUserId = securityUtils.getCurrentUserId();
        boolean isAdmin = securityUtils.hasRole("ROLE_ADMIN");

        ReportStatus newStatus = ReportStatus.valueOf(dto.status().toUpperCase());

        // Validar la transición de estado
        validateStateTransition(report, newStatus, dto.rejectionMessage(), isAdmin, currentUserId);

        // Registrar historial y actualizar el reporte
        updateReportWithNewStatus(report, newStatus);

        log.info("Estado del reporte {} actualizado a: {}", reportId, newStatus);
    }


    /**
     * Alterna el voto (like) de un usuario sobre un reporte. Si el usuario ya votó, se elimina su voto;
     * de lo contrario, se agrega su voto.
     *
     * @param reportId      Identificador del reporte a votar.
     * @throws ReportNotFoundException Si el reporte no se encuentra.
     */
    @Transactional
    @Override
    public void toggleReportVote(String reportId) {
        log.info("Iniciando toggle de voto para reporte con ID: {}", reportId);

        // Obtener reporte
        Report report = findReportById(parseObjectId(reportId));

        String currentUserId = securityUtils.getCurrentUserId();
        ObjectId userObjectId = parseObjectId(currentUserId);

        // Alternar voto
        updateVoteStatus(report, userObjectId);

        log.info("Reporte {} actualizado. Votos importantes actuales: {}", reportId, report.getImportantVotes());
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
    @Override
    public CommentPaginatedResponse getCommentsByReportId(String reportId, int page, int size) {
        log.info("Validando existencia del reporte con ID: {}", reportId);

        // Validar la existencia del reporte antes de proceder
        findReportById(parseObjectId(reportId));

        // Obtener comentarios del reporte de forma paginada
        log.info("Obteniendo comentarios para el reporte con ID: {}", reportId);
        return commentService.getCommentsByReportId(reportId, page, size);
    }


    /**
     * Genera una entrada en el historial de cambios de estado de un reporte.
     *
     * @param report       El reporte al que se le cambiará el estado.
     * @param reportStatus El nuevo estado que tendrá el reporte.
     */
    private void createHistoryReport(Report report, ReportStatus reportStatus) {
        log.info("Generando historia de cambio de estado del reporte con ID: {}", report.getId());

        String currentUserId = securityUtils.getCurrentUserId();
        ObjectId userObjectId = parseObjectId(currentUserId);

        // Registro de detalles del cambio de estado en el log
        log.info("El usuario con ID: {} está cambiando el estado del reporte: {} a {}", currentUserId,
                report.getId(), reportStatus);

        // Crear el historial del cambio de estado del reporte
        reportStatusHistoryService.createHistory(report.getId(), userObjectId, report.getReportStatus(), reportStatus);
    }



    /**
     * Valida que las coordenadas geográficas estén dentro del rango válido.
     *
     * @param latitude  Latitud (-90 a 90).
     * @param longitude Longitud (-180 a 180).
     * @throws IllegalArgumentException si alguna coordenada es inválida.
     */
    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            log.error("Latitud fuera de rango: {}", latitude);
            throw new IllegalArgumentException("Latitud fuera de rango (-90 a 90)");
        }
        if (longitude < -180 || longitude > 180) {
            log.error("Longitud fuera de rango: {}", longitude);
            throw new IllegalArgumentException("Longitud fuera de rango (-180 a 180)");
        }
    }


    /**
     * Mapea una página de reportes a un objeto de respuesta paginada.
     *
     * @param page        Página de reportes obtenida desde la base de datos.
     * @param currentPage Número de la página actual.
     * @return Respuesta paginada con los reportes y la información de paginación.
     */
    private PaginatedReportResponse mapToPaginatedResponse(Page<Report> page, int currentPage) {
        List<ReportResponse> content = reportMapper.toResponseList(page.getContent());

        log.debug("Mapeando página de reportes. Página: {}, Total de reportes: {}", currentPage, page.getTotalElements());

        return new PaginatedReportResponse(
                content,
                currentPage,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }


    /**
     * Convierte un string a un ObjectId, lanzando una excepción si el formato es inválido.
     *
     * @param id ID del reporte en formato String.
     * @return ObjectId generado a partir del String.
     * @throws IdInvalidException Si el ID no es válido.
     */
    private ObjectId parseObjectId(String id) {
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException e) {
            log.error("ID inválido: {}", id);
            throw new IdInvalidException("ID no válido para un ObjectId. ID proporcionado: " + id);
        }
    }


    /**
     * Busca un reporte por su ID. Lanza una excepción si el reporte no se encuentra.
     *
     * @param reportId ID del reporte a buscar.
     * @return El reporte encontrado.
     * @throws ReportNotFoundException Si el reporte no existe.
     */
    private Report findReportById(ObjectId reportId) {
        log.info("Buscando reporte con ID: {}", reportId);

        return reportRepository.findById(reportId)
                .orElseThrow(() -> {
                    log.warn("Reporte no encontrado con ID: {}", reportId);
                    return new ReportNotFoundException(reportId.toString());
                });
    }


    /**
     * Valida la transición de estado de un reporte, asegurando que la transición es válida
     * y que el usuario tiene permisos. Si el estado es REJECTED, se registra un mensaje de advertencia.
     *
     * @param report           El reporte a validar.
     * @param newStatus        El nuevo estado que se quiere asignar.
     * @param rejectionMessage El mensaje de rechazo, si corresponde.
     * @param isAdmin          Si el usuario tiene permisos de administrador.
     * @param currentUserId    El ID del usuario actual.
     */
    private void validateStateTransition(Report report, ReportStatus newStatus, String rejectionMessage, boolean isAdmin, String currentUserId) {
        log.info("Validando transición de estado para el reporte con ID: {}", report.getId());

        // Validar transición usando el validador
        validator.validate(report, newStatus, rejectionMessage, isAdmin, currentUserId);

        if (newStatus == ReportStatus.REJECTED) {
            log.warn("Reporte {} rechazado por el administrador. Motivo: {}", report.getId(), rejectionMessage);
        }
    }


    /**
     * Actualiza el estado de un reporte y crea un historial de la transición de estado.
     *
     * @param report    El reporte a actualizar.
     * @param newStatus El nuevo estado que se asignará al reporte.
     */
    private void updateReportWithNewStatus(Report report, ReportStatus newStatus) {
        log.info("Actualizando estado del reporte con ID: {} a {}", report.getId(), newStatus);

        // Crear historial del cambio de estado
        createHistoryReport(report, newStatus);

        // Actualizar estado y guardar el reporte
        report.setReportStatus(newStatus);
        reportRepository.save(report);
        log.info("Estado del reporte {} actualizado a {}", report.getId(), newStatus);
    }



    /**
     * Actualiza el estado del voto (like) de un usuario sobre un reporte. Si el usuario ya votó, su voto será eliminado,
     * de lo contrario, se agregará un nuevo voto.
     *
     * @param report        El reporte sobre el que se va a actualizar el voto.
     * @param userObjectId El ID del usuario que está realizando la acción.
     */
    private void updateVoteStatus(Report report, ObjectId userObjectId) {
        log.info("Actualizando voto del usuario {} para el reporte {}", userObjectId, report.getId());

        // Obtener los usuarios que ya han votado
        Set<ObjectId> likedUserIds = getLikedUserIds(report);

        if (likedUserIds.contains(userObjectId)) {
            // El usuario ya ha votado: quitar su voto
            removeVote(report, likedUserIds, userObjectId);
        } else {
            // El usuario no ha votado: agregar su voto
            addVote(report, likedUserIds, userObjectId);
        }

        // Guardar el reporte actualizado
        reportRepository.save(report);
        log.info("Reporte {} actualizado. Votos importantes: {}", report.getId(), report.getImportantVotes());
    }


    /**
     * Obtiene los IDs de los usuarios que han votado un reporte. Si no existen, inicializa una nueva colección.
     *
     * @param report El reporte del cual obtener los votos.
     * @return Un conjunto de IDs de los usuarios que han votado.
     */
    private Set<ObjectId> getLikedUserIds(Report report) {
        Set<ObjectId> likedUserIds = report.getLikedUserIds();

        if (likedUserIds == null) {
            likedUserIds = new HashSet<>();
            report.setLikedUserIds(likedUserIds);
        }

        return likedUserIds;
    }


    /**
     * Elimina el voto de un usuario en un reporte y ajusta el contador de votos importantes.
     *
     * @param report        El reporte en el que se eliminará el voto.
     * @param likedUserIds  El conjunto de usuarios que han votado.
     * @param userObjectId  El ID del usuario cuyo voto se eliminará.
     */
    private void removeVote(Report report, Set<ObjectId> likedUserIds, ObjectId userObjectId) {
        likedUserIds.remove(userObjectId);
        report.setImportantVotes(report.getImportantVotes() - 1);
        log.info("Se ha quitado el voto del usuario {} para el reporte {}", userObjectId, report.getId());
    }


    /**
     * Agrega un voto (like) de un usuario a un reporte y ajusta el contador de votos importantes.
     *
     * @param report        El reporte en el que se agregará el voto.
     * @param likedUserIds  El conjunto de usuarios que han votado.
     * @param userObjectId  El ID del usuario que está votando.
     */
    private void addVote(Report report, Set<ObjectId> likedUserIds, ObjectId userObjectId) {
        likedUserIds.add(userObjectId);
        report.setImportantVotes(report.getImportantVotes() + 1);
        log.info("Se ha sumado un voto del usuario {} para el reporte {}", userObjectId, report.getId());
    }




}
