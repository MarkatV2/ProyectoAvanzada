package co.edu.uniquindio.proyecto.service.mapper;

import co.edu.uniquindio.proyecto.dto.notification.NotificationCreateDTO;
import co.edu.uniquindio.proyecto.dto.notification.NotificationDTO;
import co.edu.uniquindio.proyecto.entity.comment.Comment;
import co.edu.uniquindio.proyecto.entity.notification.Notification;
import co.edu.uniquindio.proyecto.entity.notification.NotificationType;
import co.edu.uniquindio.proyecto.entity.report.Report;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationDTO toDTO(Notification notification);

    List<NotificationDTO> toDTOList(List<Notification> notifications);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "read", constant = "false")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    Notification fromCreateDTO(NotificationCreateDTO dto);

    // 👇 ¡Este es el estilo declarativo!
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "title", expression = "java(\"Nuevo reporte cerca de ti\")")
    @Mapping(target = "message", expression = "java(\"Se ha publicado un reporte en tu zona: '\" + report.getTitle() + \"'\")")
    @Mapping(target = "reportId", expression = "java(report.getId().toString())")
    @Mapping(target = "type", expression = "java(NotificationType.NEW_REPORT)")
    @Mapping(target = "location", source = "report.location")
    NotificationCreateDTO buildFromReportForNearbyUser(Report report, String userId);

    @Mapping(target = "userId", expression = "java(report.getUserId())")
    @Mapping(target = "title", expression = "java(\"Nuevo comentario en tu reporte\")")
    @Mapping(target = "message", expression = "java(\"El usuario \" + commenterName + \" comentó: \\\"\" + comment.getComment() + \"\\\"\")")
    @Mapping(target = "reportId", expression = "java(report.getId().toString())")
    @Mapping(target = "type", expression = "java(NotificationType.COMMENT)")
    @Mapping(target = "location", source = "report.location")
    NotificationCreateDTO buildFromComment(Comment comment, Report report, String commenterName);


}

