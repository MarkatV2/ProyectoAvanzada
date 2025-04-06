package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.comment.Comment;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface CommentRepository extends MongoRepository<Comment, ObjectId> {


    /**
     * Busca comentarios por reportId de forma paginada.
     *
     * @param reportId Identificador del reporte.
     * @param pageable Información de paginación.
     * @return Página de comentarios.
     */
    @Query("{ '_id': ?0, 'commentStatus': 'PUBLISHED' }")
    Page<Comment> findByReportId(ObjectId reportId, Pageable pageable);

    @Override
    @Query("{ '_id': ?0, 'commentStatus': 'PUBLISHED' }")
    Optional<Comment> findById(ObjectId id);
}
