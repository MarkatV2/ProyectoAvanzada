package co.edu.uniquindio.proyecto.service.mapper;

import co.edu.uniquindio.proyecto.dto.report.ReportSummaryDTO;
import co.edu.uniquindio.proyecto.entity.report.Report;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReportSummaryMapper {


    List<ReportSummaryDTO> toReportSummaryDto (List<Report> reports);

    @Mapping(target = "reportId", source = "id", qualifiedByName = "objectIdToString")
    @Mapping(target = "latitude", source = "location.y")
    @Mapping(target = "longitude", source = "location.x")
    ReportSummaryDTO toReportSummaryDto (Report report);

    @Named("objectIdToString")
    default String objectIdToString(ObjectId id) {
        return id != null ? id.toString() : null;
    }

}
