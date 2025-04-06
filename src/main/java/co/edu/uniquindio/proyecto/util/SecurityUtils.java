package co.edu.uniquindio.proyecto.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Utilidad de seguridad para extraer información del contexto de autenticación.
 * <p>
 * Proporciona métodos para obtener el ID del usuario actual, verificar si el usuario
 * es el mismo que el solicitado o si posee el rol de administrador, y para validar roles.
 * </p>
 */
@Component
@Slf4j
public class SecurityUtils {

    /**
     * Obtiene el identificador del usuario actual a partir del contexto de seguridad.
     *
     * @return El ID del usuario actual o {@code null} si no se encuentra la autenticación.
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (log.isDebugEnabled()) {
            log.debug("Obteniendo autenticación actual: {}", authentication);
        }
        String userId = extractUserIdFromAuthentication(authentication);
        if (log.isDebugEnabled()) {
            log.debug("UserId extraído: {}", userId);
        }
        return userId;
    }

    /**
     * Verifica si el usuario solicitado es el mismo que el usuario autenticado o si el usuario
     * autenticado posee el rol de administrador.
     *
     * @param requestedUserId ID del usuario sobre el cual se realiza la verificación.
     * @return {@code true} si el usuario autenticado es el mismo o es administrador; de lo contrario, {@code false}.
     */
    public boolean isSelfOrAdmin(String requestedUserId) {
        String currentUserId = getCurrentUserId();
        boolean isAdmin = hasRole("ROLE_ADMIN");
        if (log.isDebugEnabled()) {
            log.debug("Verificando acceso: requestedUserId={}, currentUserId={}, isAdmin={}",
                    requestedUserId, currentUserId, isAdmin);
        }
        return requestedUserId.equals(currentUserId) || isAdmin;
    }

    /**
     * Extrae el ID del usuario a partir de la autenticación actual.
     * <p>
     * Se espera que el principal sea una instancia de {@code Jwt} que contenga la reclamación "userId".
     * </p>
     *
     * @param authentication Contexto de autenticación.
     * @return El ID del usuario si se encuentra; de lo contrario, {@code null}.
     */
    private String extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            log.warn("No se encontró autenticación en el contexto de seguridad.");
            return null;
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String userId = jwt.getClaimAsString("userId");
            if (log.isDebugEnabled()) {
                log.debug("Extrayendo userId del Jwt: {}", userId);
            }
            return userId;
        }
        log.warn("El principal de la autenticación no es una instancia de Jwt: {}", authentication.getPrincipal());
        return null;
    }

    /**
     * Verifica si el usuario autenticado posee el rol especificado.
     *
     * @param role Rol a verificar (por ejemplo, "ROLE_ADMIN").
     * @return {@code true} si el rol se encuentra entre las autoridades del usuario; de lo contrario, {@code false}.
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.warn("No se encontró autenticación al verificar el rol: {}", role);
            return false;
        }
        boolean hasRole = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(role));
        if (log.isDebugEnabled()) {
            log.debug("El usuario '{}' tiene el rol '{}': {}",
                    authentication.getName(), role, hasRole);
        }
        return hasRole;
    }

    /**
     * Obtiene el nombre de usuario (username) del usuario autenticado a partir del JWT.
     *
     * @return El username extraído del JWT o {@code null} si no se encuentra.
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (log.isDebugEnabled()) {
            log.debug("Obteniendo autenticación actual para username: {}", authentication);
        }
        if (authentication == null) {
            log.warn("No se encontró autenticación en el contexto de seguridad.");
            return null;
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String username = jwt.getSubject(); // El subject generalmente contiene el username
            if (log.isDebugEnabled()) {
                log.debug("Username extraído del JWT: {}", username);
            }
            return username;
        }
        log.warn("El principal de la autenticación no es una instancia de Jwt: {}", authentication.getPrincipal());
        return null;
    }

}
