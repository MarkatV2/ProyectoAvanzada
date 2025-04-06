package co.edu.uniquindio.proyecto.service.mapper;

import co.edu.uniquindio.proyecto.dto.report.ReportRequest;
import co.edu.uniquindio.proyecto.dto.report.ReportResponse;
import co.edu.uniquindio.proyecto.entity.report.Report;
import org.bson.types.ObjectId;
import org.mapstruct.*;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Mapper actualizado
@Mapper(componentModel = "spring",
        imports = {LocalDateTime.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReportMapper {


    @Mapping(target = "location", expression = "java(toGeoJsonPoint(request.latitude(), request.longitude()))")
    @Mapping(target = "reportStatus", constant = "PENDING")
    @Mapping(target = "importantVotes", constant = "0")
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    Report toEntity(ReportRequest request);


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
    void updateEntity(@MappingTarget Report report, ReportRequest request);


    @Named("objectIdToString")
    default String objectIdToString(ObjectId id) {
        return id != null ? id.toString() : null;
    }

    default GeoJsonPoint toGeoJsonPoint(double latitud, double longitud) {
        return new GeoJsonPoint(longitud, latitud); // MongoDB usa (longitud, latitud)
    }

}
