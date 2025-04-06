package co.edu.uniquindio.proyecto.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CheckOwnerOrAdmin {

    /**
     * Clase de la entidad que se quiere proteger.
     * Debe tener un campo llamado "userId".
     *
     * @return clase de la entidad
     */
    Class<?> entityClass();

}


