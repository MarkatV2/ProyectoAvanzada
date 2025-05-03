package co.edu.uniquindio.proyecto.service.mapper;

import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageUploadRequest;
import co.edu.uniquindio.proyecto.entity.image.Image;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDate;

@Mapper(componentModel = "spring",
        imports = {LocalDate.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ImageMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "objectIdToString")
    @Mapping(target = "uploadDate", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    ImageResponse toImageResponse(Image image);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uploadDate", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "reportId", source = "image.reportId", qualifiedByName = "stringToObjectId")
    @Mapping(target = "userId", source = "userId", qualifiedByName = "stringToObjectId")
    Image toImage(ImageUploadRequest image, String userId);

    @Named("objectIdToString")
    default String objectIdToString(ObjectId id) {
        return id != null ? id.toString() : null;
    }

    @Named("stringToObjectId")
    default ObjectId StringToObjectId(String id){ return id != null ? new ObjectId(id) : null;}


}
