package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.category.Category;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad {@link Category}, maneja las operaciones de acceso a datos
 * en la colección de categorías en MongoDB.
 */
public interface CategoryRepository extends MongoRepository<Category, ObjectId> {

    /**
     * Obtiene todas las categorías que están activadas.
     *
     * @return Lista de categorías activadas.
     */
    List<Category> findAllByActivatedTrue();

    List<Category> findAll();

    /**
     * Obtiene una categoría activada por su identificador único.
     *
     * @param objectId Identificador de la categoría.
     * @return Categoría activada si existe, {@link Optional#empty()} si no se encuentra.
     */
    Optional<Category> findByIdAndActivatedTrue(ObjectId objectId);


    /**
     * Verifica si existe una categoría con un nombre específico.
     *
     * @param name Nombre de la categoría a buscar.
     * @return {@code true} si la categoría existe, {@code false} de lo contrario.
     */
    boolean existsByName(String name);
}
