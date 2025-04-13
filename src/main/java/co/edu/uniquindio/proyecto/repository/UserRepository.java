package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.user.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Repositorio para operaciones de acceso a datos de usuarios.
 * <p>
 * Este repositorio gestiona únicamente los usuarios con estado de cuenta distinto a {@code DELETED},
 * permitiendo mantener la integridad del sistema y la visibilidad de los usuarios activos o suspendidos.
 *
 * <p><strong>Principio de Responsabilidad Única (SRP):</strong> Esta interfaz tiene como única responsabilidad
 * el acceso y consulta de datos de usuarios desde la base de datos MongoDB.</p>
 *
 * <p>Se implementan filtros para evitar recuperar usuarios marcados como eliminados
 * (soft delete), asegurando una vista consistente de usuarios activos en toda la aplicación.</p>
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Obtiene una lista paginada de todos los usuarios cuyo estado de cuenta no es {@code DELETED}.
     *
     * @param pageable Información de paginación.
     * @return Página de usuarios activos o suspendidos.
     */
    @Query("{ 'accountStatus' : { $ne: 'DELETED' } }")
    Page<User> findAll(Pageable pageable);

    /**
     * Busca un usuario por su ID solo si su estado de cuenta no es {@code DELETED}.
     *
     * @param id ID del usuario.
     * @return Un {@link Optional} que contiene el usuario si existe y no está eliminado.
     */
    @Query("{ '_id' : ?0, 'accountStatus' : { $ne: 'DELETED' } }")
    Optional<User> findById(ObjectId id);

    /**
     * Busca un usuario por su correo electrónico solo si su estado de cuenta no es {@code DELETED}.
     *
     * @param email Correo electrónico del usuario.
     * @return Un {@link Optional} que contiene el usuario si existe y no está eliminado.
     */
    @Query("{ 'email' : ?0, 'accountStatus' : { $ne: 'DELETED' } }")
    Optional<User> findByEmail(String email);
}