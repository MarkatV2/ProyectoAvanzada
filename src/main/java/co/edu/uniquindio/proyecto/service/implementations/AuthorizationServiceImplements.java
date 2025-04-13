package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.service.interfaces.AuthorizationService;
import co.edu.uniquindio.proyecto.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * Implementación del servicio de autorización encargado de validar
 * si el usuario autenticado tiene permisos para realizar una acción específica.
 *
 * <p>La inyección se realiza por constructor gracias a {@link RequiredArgsConstructor},
 * y el componente está registrado en el contexto de Spring como "authorizationService".</p>
 */
@Service("authorizationService")
@Slf4j
@RequiredArgsConstructor
public class AuthorizationServiceImplements implements AuthorizationService {

    private final SecurityUtils securityUtils;

    /**
     * Verifica si el usuario autenticado es el mismo que el solicitado o si posee el rol ADMIN.
     *
     * <p>Utilizado comúnmente para permitir que los usuarios accedan o modifiquen sus propios datos
     * o para que administradores accedan a cualquier recurso.</p>
     *
     * @param userId Identificador del usuario objetivo.
     * @return {@code true} si el usuario actual es el mismo o posee rol de administrador.
     */
    @Override
    public boolean isSelfOrAdmin(String userId) {
        log.info("Verificando si el usuario actual es el mismo o tiene permisos de administrador. userId: {}", userId);

        boolean result = securityUtils.isSelfOrAdmin(userId);

        if (result) {
            log.debug("Autorización concedida para el usuario: {}", userId);
        } else {
            log.warn("Autorización denegada para el usuario: {}", userId);
        }

        return result;
    }

    /**
     * Verifica si el usuario autenticado coincide exactamente con el proporcionado.
     *
     * <p>Se recomienda este método cuando solo se desea permitir acceso exclusivo al propietario del recurso.</p>
     *
     * @param userId Identificador del usuario objetivo.
     * @return {@code true} si el usuario autenticado coincide con el userId.
     */
    @Override
    public boolean isSelf(String userId) {
        log.info("Verificando coincidencia con usuario autenticado. userId objetivo: {}", userId);

        String currentUserId = securityUtils.getCurrentUserId();
        boolean result = userId != null && userId.equals(currentUserId);

        if (result) {
            log.debug("El usuario autenticado ({}) coincide con el solicitado", currentUserId);
        } else {
            log.warn("El usuario autenticado ({}) no coincide con el solicitado ({})", currentUserId, userId);
        }

        return result;
    }
}