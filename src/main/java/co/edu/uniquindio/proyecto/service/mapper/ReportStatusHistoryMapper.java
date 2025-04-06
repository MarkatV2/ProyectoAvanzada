package co.edu.uniquindio.proyecto.service.mapper;

import co.edu.uniquindio.proyecto.dto.report.ReportStatusHistoryResponse;
import co.edu.uniquindio.proyecto.entity.report.ReportStatus;
import co.edu.uniquindio.proyecto.entity.report.ReportStatusHistory;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReportStatusHistoryMapper {

    // Mapeo para respuesta individual
    @Mapping(target = "id", source = "id", qualifiedByName = "objectIdToString")
    @Mapping(target = "reportId", source = "reportId", qualifiedByName = "objectIdToString")
    @Mapping(target = "userId", source = "userId", qualifiedByName = "objectIdToString")
    ReportStatusHistoryResponse toResponse(ReportStatusHistory history);

    // Mapeo para creación de la entidad a partir del DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "changedAt", expression = "java(java.time.LocalDateTime.now())")
    ReportStatusHistory toEntity(ObjectId reportId, ObjectId userId, ReportStatus previousStatus, ReportStatus newStatus);


    @Named("objectIdToString")
    default String objectIdToString(ObjectId id) {
        return id != null ? id.toString() : null;
    }
}

