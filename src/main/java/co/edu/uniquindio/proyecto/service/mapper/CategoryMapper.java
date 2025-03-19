package co.edu.uniquindio.proyecto.service.mapper;

import co.edu.uniquindio.proyecto.dto.category.CategoryRequest;
import co.edu.uniquindio.proyecto.dto.category.CategoryResponse;
import co.edu.uniquindio.proyecto.entity.category.Category;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring",
        imports = {LocalDate.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "objectIdToString")
    @Mapping(target = "dateCreation", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    CategoryResponse toCategoryResponse(Category category);

    // Nuevo método para actualización
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "activated", ignore = true)
    void updateCategoryFromRequest(CategoryRequest request, @MappingTarget Category category);

    @Mapping(target = "id", ignore = true) // MongoDB generará el ID
    @Mapping(target = "activated", constant = "true")
    @Mapping(target = "dateCreation", expression = "java(java.time.LocalDateTime.now())")
    Category toEntity(CategoryRequest request);

    @Named("objectIdToString")
    default String objectIdToString(ObjectId id) {
        return id != null ? id.toString() : null;
    }

}