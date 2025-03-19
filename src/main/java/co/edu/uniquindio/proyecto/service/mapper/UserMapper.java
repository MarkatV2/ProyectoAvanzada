package co.edu.uniquindio.proyecto.service.mapper;


import co.edu.uniquindio.proyecto.dto.user.UserRegistration;
import co.edu.uniquindio.proyecto.dto.user.UserResponse;
import co.edu.uniquindio.proyecto.dto.user.UserUpdateRequest;
import co.edu.uniquindio.proyecto.entity.user.AccountStatus;
import co.edu.uniquindio.proyecto.entity.user.Rol;
import co.edu.uniquindio.proyecto.entity.user.User;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.*;
import java.time.LocalDate;

@Mapper(componentModel = "spring",
        imports = {LocalDate.class, Rol.class, AccountStatus.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    // Mapeo a UserResponse (existente)
    @Mapping(target = "accountStatus", expression = "java(user.getAccountStatus().toString())")
    @Mapping(target = "id", source = "id", qualifiedByName = "objectIdToString")
    UserResponse toUserResponse(User user);

    @Named("objectIdToString")
    default String objectIdToString(ObjectId id) {
        return id != null ? id.toString() : null;
    }

    // Mapeo desde UserRegistration (nuevo)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", expression = "java( new org.springframework.security.crypto.bcrypt." +
            "BCryptPasswordEncoder().encode(userRegistration.password()) )")
    @Mapping(target = "dateCreation", expression = "java(LocalDate.now())")
    @Mapping(target = "rol", expression = "java(Rol.USER)")
    @Mapping(target = "accountStatus", expression = "java(AccountStatus.REGISTERED)")
    User toUserEntity(UserRegistration userRegistration);


    // Nuevo método para actualizar
    @Mapping(target = "id", ignore = true) // No se actualiza el ID
    @Mapping(target = "password", ignore = true) // No se actualiza la contraseña
    @Mapping(target = "dateCreation", ignore = true) // No se actualiza la fecha de creación
    @Mapping(target = "rol", ignore = true) // No se actualiza el rol
    @Mapping(target = "accountStatus", ignore = true) // No se actualiza el estado de la cuenta
    void updateUserFromRequest(UserUpdateRequest userUpdateRequest, @MappingTarget User user);
}