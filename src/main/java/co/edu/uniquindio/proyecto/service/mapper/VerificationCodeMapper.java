package co.edu.uniquindio.proyecto.service.mapper;

import co.edu.uniquindio.proyecto.entity.auth.VerificationCode;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

@Mapper(componentModel = "spring",
        imports = {LocalDate.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VerificationCodeMapper {

    @Mapping(target = "code", source = "code")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "expiresAt", expression = "java(java.time.LocalDateTime.now().plusMinutes(expirationMinutes))")
    VerificationCode toVerificationCode(String code, User user, int expirationMinutes);

}