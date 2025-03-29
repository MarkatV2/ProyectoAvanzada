package co.edu.uniquindio.proyecto.service.mapper;

import co.edu.uniquindio.proyecto.dto.comment.CommentRequest;
import co.edu.uniquindio.proyecto.dto.comment.CommentResponse;
import co.edu.uniquindio.proyecto.entity.comment.Comment;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "reportId", source = "reportId", qualifiedByName = "stringToObjectId")
    @Mapping(target = "userId", source = "userId", qualifiedByName = "stringToObjectId")
    Comment toEntity(CommentRequest request, String reportId, String userId, String userName);

    @Mapping(target = "id", expression = "java(entity.getId().toString())")
    @Mapping(target = "userId", expression = "java(entity.getUserId().toString())")
    @Mapping(target = "reportId", expression = "java(entity.getReportId().toString())")
    CommentResponse toResponse(Comment entity);

    @Named("stringToObjectId")
    default ObjectId stringToObjectId(String id){ return id != null ? new ObjectId(id) : null;}
}
