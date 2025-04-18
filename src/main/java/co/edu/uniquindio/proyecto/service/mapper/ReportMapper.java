package co.edu.uniquindio.proyecto.service.mapper;

import co.edu.uniquindio.proyecto.dto.report.ReportRequest;
import co.edu.uniquindio.proyecto.dto.report.ReportResponse;
import co.edu.uniquindio.proyecto.entity.report.Report;
import org.bson.types.ObjectId;
import org.mapstruct.*;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import java.time.LocalDateTime;
import java.util.List;

// Mapper actualizado
@Mapper(componentModel = "spring",
        imports = {LocalDateTime.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReportMapper {


    List<ReportResponse> toResponseList(List<Report> reports);

    @Mapping(target = "location", expression = "java(toGeoJsonPoint(request.latitude(), request.longitude()))")
    @Mapping(target = "reportStatus", constant = "PENDING")
    @Mapping(target = "importantVotes", constant = "0")
    @Mapping(target = "userId", source = "userId", qualifiedByName = "stringToObjectId")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "userEmail", source = "currentUsername")
    Report toEntity(ReportRequest request, String userId, String currentUsername);


    @Mapping(target = "id", source = "id", qualifiedByName = "objectIdToString")
    @Mapping(target = "latitude", source = "location.y")
    @Mapping(target = "longitude", source = "location.x")
    ReportResponse toResponse(Report report);

    // Actualización parcial (ignora campos sensibles)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "reportStatus", ignore = true)
    @Mapping(target = "importantVotes", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userEmail", ignore = true)
    void updateEntity(@MappingTarget Report report, ReportRequest request);


    @Named("objectIdToString")
    default String objectIdToString(ObjectId id) {
        return id != null ? id.toString() : null;
    }

    @Named("stringToObjectId")
    default ObjectId stringToObjectId(String id){ return id != null ? new ObjectId(id) : null;}


    default GeoJsonPoint toGeoJsonPoint(double latitud, double longitud) {
        return new GeoJsonPoint(longitud, latitud); // MongoDB usa (longitud, latitud)
    }

}
