package co.edu.uniquindio.proyecto.aspect;

import co.edu.uniquindio.proyecto.annotation.CheckOwnerOrAdmin;
import co.edu.uniquindio.proyecto.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthorizationAspect {

    private final MongoTemplate mongoTemplate;
    private final SecurityUtils securityUtils;

    /**
     * Intercepta métodos anotados con {@link CheckOwnerOrAdmin} para validar si el usuario autenticado
     * es el propietario del recurso o tiene rol de administrador.
     */
    @Before("@annotation(checkOwnerOrAdmin) && args(id,..)")
    public void checkOwnerOrAdminAccess(CheckOwnerOrAdmin checkOwnerOrAdmin, String id) {
        Class<?> entityClass = checkOwnerOrAdmin.entityClass();

        log.info("Verificando acceso a entidad {} con ID {}", entityClass.getSimpleName(), id);

        Object entity = mongoTemplate.findById(id, entityClass);

        String currentUserId = securityUtils.getCurrentUserId();
        boolean isAdmin = securityUtils.hasRole("ROLE_ADMIN");

        try {
            Field userIdField = entityClass.getDeclaredField("userId");
            userIdField.setAccessible(true);
            String ownerId = (String) userIdField.get(entity);

            log.debug("ID actual: {}, ID del recurso: {}, ¿es admin?: {}", currentUserId, ownerId, isAdmin);

            if (!currentUserId.equals(ownerId) && !isAdmin) {
                log.warn("Acceso denegado: el usuario {} no es dueño ni administrador", currentUserId);
                throw new AccessDeniedException("No tienes permisos para acceder a este recurso.");
            }

        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Error al acceder al campo 'userId' en {}", entityClass.getSimpleName(), e);
            throw new RuntimeException("Error al verificar permisos.");
        }
    }
}
