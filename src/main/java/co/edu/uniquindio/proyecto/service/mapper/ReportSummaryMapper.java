package co.edu.uniquindio.proyecto.service.mapper;

import co.edu.uniquindio.proyecto.dto.report.ReportSummaryDTO;
import co.edu.uniquindio.proyecto.entity.category.CategoryRef;
import co.edu.uniquindio.proyecto.entity.report.Report;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReportSummaryMapper {


    List<ReportSummaryDTO> toReportSummaryDto (List<Report> reports);

    @Mapping(target = "reportId",    source = "id", qualifiedByName = "objectIdToString")
    @Mapping(target = "latitude",    source = "location.y")
    @Mapping(target = "longitude",   source = "location.x")
    @Mapping(target = "categoryNames", source = "categoryList", qualifiedByName = "mapCategoryNames")
    @Mapping(target = "status",      expression = "java(report.getReportStatus().name())")
    ReportSummaryDTO toReportSummaryDto(Report report);


    @Named("objectIdToString")
    default String objectIdToString(ObjectId id) {
        return id != null ? id.toString() : null;
    }

    @Named("mapCategoryNames")
    default List<String> mapCategoryNames(List<CategoryRef> categoryRefs) {
        if (categoryRefs == null) {
            return List.of(); // evitar null
        }
        return categoryRefs.stream()
                .map(CategoryRef::getName)
                .filter(Objects::nonNull)
                .toList();
    }


}
