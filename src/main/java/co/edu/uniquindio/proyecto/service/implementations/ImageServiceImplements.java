package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageUploadRequest;
import co.edu.uniquindio.proyecto.entity.image.Image;
import co.edu.uniquindio.proyecto.exception.IdInvalidException;
import co.edu.uniquindio.proyecto.exception.ImageNotFoundException;
import co.edu.uniquindio.proyecto.exception.InvalidImageException;
import co.edu.uniquindio.proyecto.exception.ReportNotFoundException;
import co.edu.uniquindio.proyecto.repository.ImageRepository;
import co.edu.uniquindio.proyecto.repository.ReportRepository;
import co.edu.uniquindio.proyecto.service.mapper.ImageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class ImageServiceImplements {

    private final ImageRepository imageRepository;
    private final ReportRepository reportRepository;
    private final ImageMapper imageMapper;

    //Obtener imagen por su id
    public ImageResponse getImageById (String id){
        ObjectId objectId = parseObjectId(id);

        log.debug("Buscando imagen ID: {}", id);
        Image image = imageRepository.findById(objectId)
                .orElseThrow(() -> {
                    log.warn("Imagen no encontrada ID: {}", id);
                    return new ImageNotFoundException("Imagen no encontrada ID:" + id);
                });

        log.info("Imagen ID: {} encontrada exitosamente", id);
        return imageMapper.toImageResponse(image);
    }


    public ImageResponse registerImage(ImageUploadRequest request) {
        log.info("Verificando Imagen...");
        validateCloudinaryUrl(request.imageUrl());
        Image image = imageMapper.toImage(request);

        return imageMapper.toImageResponse(imageRepository.save(image));
    }

    public void deleteImage(String id) {
        ObjectId objectId = parseObjectId(id);

        log.debug("Buscando imagen ID: {}", id);
        Image image = imageRepository.findById(objectId)
                .orElseThrow(() -> {
                    log.warn("Imagen no encontrada ID: {}", id);
                    return new ImageNotFoundException(id);
                });

        log.debug("Eliminando imagen ID: {} como inactiva", id);
        imageRepository.delete(image);
        log.info("Imagen ID: {} Eliminada exitosamente", id);
    }

    private void validateCloudinaryUrl(String url) { //falta el handler de esta
        if (!url.matches("^https://res.cloudinary.com/.*/(image|video)/upload/.*")) {
            log.error("URL no válida: {}", url);
            throw new InvalidImageException("Formato de URL de Cloudinary inválido");
        }
    }

    public List<ImageResponse> getAllImagesByReport (ObjectId reportId){
        reportRepository.findById(reportId).orElseThrow(() -> new ReportNotFoundException(reportId.toString()));
        log.info("Imagenes Obtenidas correctamente");
        return imageRepository.findAllByReportId(reportId).stream()
                .map(imageMapper::toImageResponse).toList();
    }

    private ObjectId parseObjectId(String id) {
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException e) {
            log.error("ID inválido: {}", id);
            throw new IdInvalidException("ID no válido");
        }
    }


}
