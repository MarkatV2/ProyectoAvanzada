package co.edu.uniquindio.proyecto.security.aspect;

import co.edu.uniquindio.proyecto.annotation.CheckOwnerOrAdmin;
import co.edu.uniquindio.proyecto.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.*;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
/**
 * Aspecto que intercepta métodos anotados con {@link CheckOwnerOrAdmin}
 * para validar que el usuario autenticado sea el propietario del recurso
 * o cuente con permisos de administrador.
 *
 * <p>Este mecanismo permite aplicar reglas de seguridad reutilizables
 * sin necesidad de replicar lógica en cada método de servicio o controlador.</p>
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthorizationAspect {

    private final MongoTemplate mongoTemplate;
    private final SecurityUtils securityUtils;

    /**
     * Interceptor AOP que se ejecuta antes de los métodos anotados con {@code @CheckOwnerOrAdmin}.
     * <p>
     * Valida que el usuario sea el dueño del recurso (según el campo <code>userId</code> en la entidad)
     * o que posea el rol de administrador.
     * </p>
     *
     * @param checkOwnerOrAdmin Anotación con la clase de entidad objetivo.
     * @param id                Identificador del recurso (pasado como primer argumento al método).
     * @throws AccessDeniedException Si el usuario no tiene permisos suficientes.
     */
    @Before("@annotation(checkOwnerOrAdmin) && args(id,..)")
    public void checkOwnerOrAdminAccess(CheckOwnerOrAdmin checkOwnerOrAdmin, String id) {
        Class<?> entityClass = checkOwnerOrAdmin.entityClass();

        log.info("Autorización: verificando acceso a {} con ID {}", entityClass.getSimpleName(), id);

        Object entity = mongoTemplate.findById(new ObjectId((id)), entityClass);

        String currentUserId = securityUtils.getCurrentUserId();
        boolean isAdmin = securityUtils.hasRole("ROLE_ADMIN");

        try {
            Field userIdField = entityClass.getDeclaredField("userId");
            userIdField.setAccessible(true);
            String ownerId = userIdField.get(entity).toString();

            log.info("Usuario actual: {}, Propietario del recurso: {}, ¿Es admin?: {}", currentUserId, ownerId, isAdmin);

            if (!currentUserId.equals(ownerId) && !isAdmin) {
                log.warn("Acceso denegado: el usuario {} no tiene permisos sobre este recurso", currentUserId);
                throw new AccessDeniedException("No tienes permisos para acceder a este recurso.");
            }

        }catch(NullPointerException ex){
            log.info("Entidad no encontrada, validando con el controlador....");
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Error accediendo al campo 'userId' en {}", entityClass.getSimpleName(), e);
            throw new RuntimeException("Error al verificar permisos.");
        }
    }
}