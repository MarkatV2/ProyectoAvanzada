package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.annotation.CheckOwnerOrAdmin;
import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageUploadRequest;
import co.edu.uniquindio.proyecto.entity.image.Image;
import org.bson.types.ObjectId;

import java.util.List;

/**
 * Servicio para gestionar imágenes asociadas a reportes.
 */
public interface ImageService {

    /**
     * Obtiene una imagen por su ID.
     *
     * @param id ID de la imagen.
     * @return imagen encontrada.
     */
    ImageResponse getImageById(String id);

    /**
     * Registra una nueva imagen.
     *
     * @param request datos de la imagen a registrar.
     * @return imagen registrada.
     */
    ImageResponse registerImage(ImageUploadRequest request);

    /**
     * Elimina una imagen por su ID.
     *
     * @param id ID de la imagen.
     */

    @CheckOwnerOrAdmin(entityClass = Image.class)
    void deleteImage(String id);

    /**
     * Obtiene todas las imágenes asociadas a un reporte.
     *
     * @param reportId ID del reporte.
     * @return lista de imágenes asociadas.
     */
    List<ImageResponse> getAllImagesByReport(ObjectId reportId);
}
