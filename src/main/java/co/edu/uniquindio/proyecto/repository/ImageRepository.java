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
     * @throws IllegalArgumentException si el reportId es null.
     */
    default List<Image> findAllByReportId(ObjectId reportId) {
        if (reportId == null) {
            throw new IllegalArgumentException("El reportId no puede ser nulo");
        }
        return findByReportId(reportId); // Llama al método real que consulta la base de datos
    }

    /**
     * Consulta por reportId.
     *
     * @param reportId Identificador del reporte.
     * @return Lista de imágenes asociadas al reporte.
     */
    List<Image> findByReportId(ObjectId reportId);
}

