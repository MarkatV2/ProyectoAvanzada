package co.edu.uniquindio.proyecto.service.mapper;


import co.edu.uniquindio.proyecto.dto.UserRegistration;
import co.edu.uniquindio.proyecto.dto.UserResponse;
import co.edu.uniquindio.proyecto.dto.UserUpdateRequest;
import co.edu.uniquindio.proyecto.entity.AccountStatus;
import co.edu.uniquindio.proyecto.entity.Rol;
import co.edu.uniquindio.proyecto.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.mapstruct.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDate;

@Mapper(componentModel = "spring",
        imports = {LocalDate.class, Rol.class, AccountStatus.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    // Mapeo a UserResponse (existente)
    @Mapping(target = "accountStatus", expression = "java(user.getAccountStatus().toString())")
    UserResponse toUserResponse(User user);

    // Mapeo desde UserRegistration (nuevo)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", expression = "java(encodePassword(userRegistration.password(), passwordEncoder))")
    @Mapping(target = "email", source = "email") // Mapeo explícito
    @Mapping(target = "fullName", source = "fullName") // Mapeo explícito
    @Mapping(target = "cityOfResidence", source = "cityOfResidence") // Mapeo explícito
    @Mapping(target = "dateCreation", expression = "java(LocalDate.now())")
    @Mapping(target = "rol", expression = "java(Rol.USER)")
    @Mapping(target = "accountStatus", expression = "java(AccountStatus.REGISTERED)")
    User toUserEntity(UserRegistration userRegistration, @Context PasswordEncoder passwordEncoder);

    // Método default para reutilizar la lógica de encriptación
    default String encodePassword(String rawPassword, @Context PasswordEncoder passwordEncoder) {
        return passwordEncoder.encode(rawPassword);
    }

    // Nuevo método para actualizar
    @Mapping(target = "id", ignore = true) // No se actualiza el ID
    @Mapping(target = "password", ignore = true) // No se actualiza la contraseña
    @Mapping(target = "dateCreation", ignore = true) // No se actualiza la fecha de creación
    @Mapping(target = "rol", ignore = true) // No se actualiza el rol
    @Mapping(target = "accountStatus", ignore = true) // No se actualiza el estado de la cuenta
    void updateUserFromRequest(UserUpdateRequest userUpdateRequest, @MappingTarget User user);
}