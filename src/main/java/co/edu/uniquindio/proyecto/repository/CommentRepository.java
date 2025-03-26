package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.comment.Comment;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentRepository extends MongoRepository<Comment, ObjectId> {
}
