package co.edu.uniquindio.proyecto.repository;

import co.edu.uniquindio.proyecto.entity.image.Image;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repositorio para la entidad {@link Image}, maneja las operaciones de acceso a datos
 * para las imágenes asociadas a los reportes.
 */
public interface ImageRepository extends MongoRepository<Image, ObjectId> {

    /**
     * Obtiene todas las imágenes asociadas a un reporte específico.
     *
     * @param reportId Identificador del reporte al que pertenecen las imágenes.
     * @return Lista de imágenes asociadas al reporte.
     */
    List<Image> findAllByReportId(ObjectId reportId);
}
