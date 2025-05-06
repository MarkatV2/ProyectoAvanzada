package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.dto.response.SuccessResponse;
import co.edu.uniquindio.proyecto.dto.user.*;

/**
 * Servicio para gestionar usuarios del sistema.
 */
public interface UserService {

    /**
     * Obtiene una lista paginada de usuarios.
     *
     * @param page número de página.
     * @param size tamaño de página.
     * @return usuarios paginados.
     */
    PaginatedUserResponse getUsers(int page, int size);

    /**
     * Registra un nuevo usuario.
     *
     * @param userRegistration datos de registro.
     * @return usuario registrado.
     */
    UserResponse registerUser(UserRegistration userRegistration);

    UserResponse getCurrentUser();

    /**
     * Obtiene la información de un usuario.
     *
     * @param userId ID del usuario.
     * @return usuario encontrado.
     */
    UserResponse getUser(String userId);

    /**
     * Actualiza los datos de un usuario.
     *
     * @param id                ID del usuario.
     * @param userUpdateRequest nuevos datos.
     * @return usuario actualizado.
     */
    UserResponse updateUser(String id, UserUpdateRequest userUpdateRequest);

    /**
     * Actualiza la contraseña de un usuario.
     *
     * @param id              ID del usuario.
     * @param passwordUpdate  nueva contraseña.
     * @return respuesta de éxito.
     */
    SuccessResponse updateUserPassword(String id, PasswordUpdate passwordUpdate);

    /**
     * Elimina lógicamente un usuario.
     *
     * @param id ID del usuario.
     * @return respuesta de éxito.
     */
    SuccessResponse deleteUser(String id);
}
