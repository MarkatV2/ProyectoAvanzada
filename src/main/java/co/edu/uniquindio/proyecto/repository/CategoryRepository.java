package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.category.Category;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends MongoRepository<Category, ObjectId> {

    List<Category> findByActivatedTrue();// Query derivado de Spring Data

    Optional<Category> findByActivatedTrue(ObjectId objectId);

    boolean existsByName(String name);

}