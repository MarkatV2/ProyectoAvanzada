package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.image.Image;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface ImageRepository extends MongoRepository<Image, ObjectId> {

}
