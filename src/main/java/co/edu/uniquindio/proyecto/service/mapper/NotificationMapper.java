package co.edu.uniquindio.proyecto.service.mapper;

import co.edu.uniquindio.proyecto.dto.notification.NotificationCreateDTO;
import co.edu.uniquindio.proyecto.dto.notification.NotificationDTO;
import co.edu.uniquindio.proyecto.entity.comment.Comment;
import co.edu.uniquindio.proyecto.entity.notification.Notification;
import co.edu.uniquindio.proyecto.entity.notification.NotificationType;
import co.edu.uniquindio.proyecto.entity.report.Report;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;


import java.time.LocalDateTime;
import java.util.List;

import org.bson.types.ObjectId;
import org.mapstruct.*;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "objectIdToString")
    @Mapping(target = "createdAt", source = "createdAt")
    NotificationDTO toDTO(Notification notification);

    List<NotificationDTO> toDTOList(List<Notification> notifications);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "read", constant = "false")
    @Mapping(target = "delivered", constant = "false")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    Notification fromCreateDTO(NotificationCreateDTO dto);

    @Named("objectIdToString")
    static String objectIdToString(ObjectId id) {
        return id != null ? id.toString() : null;
    }

    @Named("stringToObjectId")
    static ObjectId stringToObjectId(String id) {
        return id != null ? new ObjectId(id) : null;
    }

    default NotificationCreateDTO buildFromReportForNearbyUser(Report report, String userId) {
        if (report == null || userId == null) {
            throw new IllegalArgumentException("Report and userId cannot be null");
        }

        return new NotificationCreateDTO(
                userId,
                "Nuevo reporte cerca de ti",
                String.format("Se ha publicado un reporte en tu zona: '%s'", report.getTitle()),
                report.getId().toString(),
                NotificationType.NEW_REPORT,
                report.getLocation()
        );
    }

    default NotificationCreateDTO buildFromComment(Comment comment, Report report, String commenterName) {
        if (comment == null || report == null || commenterName == null) {
            throw new IllegalArgumentException("Comment, report and commenterName cannot be null");
        }

        return new NotificationCreateDTO(
                report.getUserId(),
                "Nuevo comentario en tu reporte",
                String.format("El usuario %s comentó: \"%s\"", commenterName, comment.getComment()),
                report.getId().toString(),
                NotificationType.COMMENT,
                report.getLocation()
        );
    }

    @AfterMapping
    default void validateNotification(@MappingTarget Notification notification) {
        if (notification.getUserId() == null || notification.getTitle() == null) {
            throw new IllegalStateException("Invalid notification mapping");
        }
    }
}
