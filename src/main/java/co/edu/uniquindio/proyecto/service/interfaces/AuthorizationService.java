package co.edu.uniquindio.proyecto.service.interfaces;

public interface AuthorizationService {

    public boolean isSelfOrAdmin(String userId);
    public boolean isSelf(String userId);
}
