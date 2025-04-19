package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.comment.Comment;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

/**
 * Repositorio para la entidad {@link Comment}, maneja las operaciones de acceso a datos
 * para los comentarios asociados a los reportes.
 */
public interface CommentRepository extends MongoRepository<Comment, ObjectId> {

    /**
     * Busca los comentarios de un reporte específico de forma paginada, solo los comentarios
     * cuyo estado es "PUBLISHED".
     *
     * @param reportId Identificador del reporte al que pertenecen los comentarios.
     * @param pageable Información de paginación.
     * @return Página de comentarios publicados asociados al reporte.
     */
    @Query("{ 'reportId': ?0, 'commentStatus': 'PUBLISHED' }")
    Page<Comment> findByAllByReportId(ObjectId reportId, Pageable pageable);

    /**
     * Obtiene un comentario específico por su ID, solo si su estado es "PUBLISHED".
     *
     * @param id Identificador del comentario.
     * @return Un comentario opcional si existe, {@link Optional#empty()} si no se encuentra.
     */
    @Override
    @Query("{ '_id': ?0, 'commentStatus': 'PUBLISHED' }")
    Optional<Comment> findById(ObjectId id);
}

