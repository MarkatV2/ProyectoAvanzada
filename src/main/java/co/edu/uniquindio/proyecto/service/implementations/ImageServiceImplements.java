package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageUploadRequest;
import co.edu.uniquindio.proyecto.entity.image.Image;
import co.edu.uniquindio.proyecto.exception.IdInvalidException;
import co.edu.uniquindio.proyecto.exception.image.ImageNotFoundException;
import co.edu.uniquindio.proyecto.exception.image.InvalidImageException;
import co.edu.uniquindio.proyecto.exception.ReportNotFoundException;
import co.edu.uniquindio.proyecto.repository.ImageRepository;
import co.edu.uniquindio.proyecto.repository.ReportRepository;
import co.edu.uniquindio.proyecto.service.interfaces.ImageService;
import co.edu.uniquindio.proyecto.service.mapper.ImageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio para la gestión de imágenes.
 * <p>
 * Proporciona operaciones para obtener, registrar y eliminar imágenes, así como para
 * recuperar todas las imágenes asociadas a un reporte. Valida que las URLs de las imágenes
 * sean válidas para Cloudinary.
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImageServiceImplements implements ImageService {

    private final ImageRepository imageRepository;
    private final ReportRepository reportRepository;
    private final ImageMapper imageMapper;

    /**
     * Recupera la información de una imagen a partir de su ID.
     *
     * @param id Identificador de la imagen.
     * @return {@code ImageResponse} con la información de la imagen.
     * @throws IdInvalidException      Si el ID no tiene un formato válido.
     * @throws ImageNotFoundException  Si no se encuentra la imagen.
     */
    public ImageResponse getImageById(String id) {
        ObjectId objectId = parseObjectId(id);
        log.debug("Buscando imagen con ID: {}", id);
        Image image = imageRepository.findById(objectId)
                .orElseThrow(() -> {
                    log.warn("Imagen no encontrada para ID: {}", id);
                    return new ImageNotFoundException("Imagen no encontrada ID: " + id);
                });
        log.info("Imagen con ID: {} encontrada exitosamente", id);
        return imageMapper.toImageResponse(image);
    }

    /**
     * Registra una nueva imagen a partir de los datos de la solicitud.
     *
     * @param request Objeto con la información para subir la imagen.
     * @return {@code ImageResponse} con la imagen registrada.
     * @throws InvalidImageException Si la URL de la imagen no es válida.
     */
    public ImageResponse registerImage(ImageUploadRequest request) {
        log.info("Verificando URL de imagen: {}", request.imageUrl());
        validateCloudinaryUrl(request.imageUrl());
        Image image = imageMapper.toImage(request);
        Image savedImage = imageRepository.save(image);
        log.info("Imagen registrada exitosamente con ID: {}", savedImage.getId());
        return imageMapper.toImageResponse(savedImage);
    }

    /**
     * Elimina una imagen, dado su ID.
     *
     * @param id Identificador de la imagen a eliminar.
     * @throws IdInvalidException     Si el ID es inválido.
     * @throws ImageNotFoundException Si la imagen no se encuentra.
     */
    public void deleteImage(String id) {
        ObjectId objectId = parseObjectId(id);
        log.debug("Buscando imagen con ID: {}", id);
        Image image = imageRepository.findById(objectId)
                .orElseThrow(() -> {
                    log.warn("Imagen no encontrada para ID: {}", id);
                    return new ImageNotFoundException("Imagen no encontrada ID: " + id);
                });
        log.debug("Eliminando imagen con ID: {}", id);
        imageRepository.delete(image);
        log.info("Imagen con ID: {} eliminada exitosamente", id);
    }

    /**
     * Valida que la URL proporcionada cumpla con el formato esperado para Cloudinary.
     *
     * @param url URL de la imagen.
     * @throws InvalidImageException Si el formato de la URL es inválido.
     */
    private void validateCloudinaryUrl(String url) {
        if (!url.matches("^https://res.cloudinary.com/.*/(image|video)/upload/.*")) {
            log.error("URL no válida: {}", url);
            throw new InvalidImageException("Formato de URL de Cloudinary inválido");
        }
    }

    /**
     * Recupera todas las imágenes asociadas a un reporte.
     *
     * @param reportId Identificador del reporte.
     * @return Lista de {@code ImageResponse} correspondientes a las imágenes del reporte.
     * @throws ReportNotFoundException Si el reporte no se encuentra.
     */
    public List<ImageResponse> getAllImagesByReport(ObjectId reportId) {
        // Verifica que el reporte existe.
        reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId.toString()));
        log.info("Recuperando imágenes para el reporte con ID: {}", reportId);
        return imageRepository.findAllByReportId(reportId).stream()
                .map(imageMapper::toImageResponse)
                .toList();
    }

    /**
     * Convierte un String a ObjectId.
     *
     * @param id Identificador en formato String.
     * @return El ObjectId correspondiente.
     * @throws IdInvalidException Si el formato del ID es inválido.
     */
    private ObjectId parseObjectId(String id) {
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException e) {
            log.error("ID inválido: {}", id);
            throw new IdInvalidException("ID no válido");
        }
    }
}
