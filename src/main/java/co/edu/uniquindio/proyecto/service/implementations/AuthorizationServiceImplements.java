package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.service.interfaces.AuthorizationService;
import co.edu.uniquindio.proyecto.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio de autorización encargado de validar que el usuario actual tenga los permisos
 * adecuados para realizar determinadas acciones, ya sea que sea el mismo usuario o un administrador.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthorizationServiceImplements implements AuthorizationService {

    private final SecurityUtils securityUtils;

    /**
     * Verifica que el usuario actual es el mismo que el usuario solicitado o bien que tenga el rol de administrador.
     *
     * @param userId Identificador del usuario sobre el que se realiza la verificación.
     * @return {@code true} si el usuario actual es el mismo o es administrador; {@code false} en caso contrario.
     */
    public boolean isSelfOrAdmin(String userId) {
        log.info("Iniciando verificación de permisos para el usuario: {}", userId);
        return securityUtils.isSelfOrAdmin(userId);
    }

    /**
     * Verifica que el usuario actual es el mismo que el usuario solicitado.
     *
     * @param userId Identificador del usuario a verificar.
     * @return {@code true} si el usuario actual coincide con el proporcionado; {@code false} en caso contrario.
     */
    public boolean isSelf(String userId) {
        log.info("Verificando que el usuario actual es el mismo que el solicitado: {}", userId);
        String currentUserId = securityUtils.getCurrentUserId();
        log.debug("Usuario actual obtenido: {}", currentUserId);
        return userId != null && userId.equals(currentUserId);
    }
}
