package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.dto.*;

public interface UserService {

    public PaginatedUserResponse getUsers(int page, int size);

    public UserResponse registerUser(UserRegistration userRegistration);

    public UserResponse getUser(String userId);
    public UserResponse updateUser(String id, UserUpdateRequest userUpdateRequest);
    public SuccessResponse updateUserPassword(String id, PasswordUpdate passwordUpdate);
    public SuccessResponse deleteUser(String id);
}