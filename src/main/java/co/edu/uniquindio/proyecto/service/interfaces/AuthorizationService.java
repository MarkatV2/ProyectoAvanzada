package co.edu.uniquindio.proyecto.service.interfaces;

/**
 * Servicio para verificar si el usuario actual tiene permisos sobre ciertos recursos,
 * ya sea por ser el mismo usuario o tener rol de administrador.
 */
public interface AuthorizationService {

    /**
     * Verifica si el usuario autenticado actualmente es el mismo que el proporcionado
     * o tiene rol de administrador.
     *
     * @param userId ID del usuario objetivo.
     * @return {@code true} si es el mismo usuario o tiene rol ADMIN, {@code false} en caso contrario.
     */
    boolean isSelfOrAdmin(String userId);

    /**
     * Verifica si el usuario autenticado actualmente es el mismo que el proporcionado.
     *
     * @param userId ID del usuario objetivo.
     * @return {@code true} si es el mismo usuario autenticado, {@code false} en caso contrario.
     */
    boolean isSelf(String userId);
}

