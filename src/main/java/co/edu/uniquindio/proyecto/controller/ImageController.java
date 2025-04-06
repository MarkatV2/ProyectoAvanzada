package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageUploadRequest;
import co.edu.uniquindio.proyecto.entity.image.Image;
import co.edu.uniquindio.proyecto.service.implementations.ImageServiceImplements;
import co.edu.uniquindio.proyecto.annotation.CheckOwnerOrAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Controlador para la gestión de imágenes.
 * <p>
 * Proporciona endpoints para obtener, registrar y eliminar imágenes.
 * </p>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/images")
public class ImageController {

    private final ImageServiceImplements imageService;

    /**
     * Obtiene la información de una imagen a partir de su ID.
     *
     * @param imageId Identificador de la imagen.
     * @return ResponseEntity con la imagen encontrada y código HTTP 200.
     */
    @GetMapping("/{imageId}")
    public ResponseEntity<ImageResponse> getImageById(@PathVariable String imageId) {
        log.info("Iniciando búsqueda de imagen con ID: {}", imageId);
        ImageResponse imageResponse = imageService.getImageById(imageId);
        log.info("Imagen con ID: {} encontrada exitosamente", imageId);
        return ResponseEntity.ok(imageResponse);
    }

    /**
     * Registra una nueva imagen a partir de los datos proporcionados.
     *
     * @param request Objeto {@code ImageUploadRequest} con los datos de la imagen.
     * @return ResponseEntity con la imagen registrada y la URI del recurso creado.
     */
    @PostMapping
    public ResponseEntity<ImageResponse> registerImage(@Valid @RequestBody ImageUploadRequest request) {
        log.info("Registrando nueva imagen desde URL: {}", request.imageUrl());
        ImageResponse savedImage = imageService.registerImage(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedImage.id())
                .toUri();
        log.info("Imagen registrada exitosamente con ID: {}", savedImage.id());
        return ResponseEntity.created(location).body(savedImage);
    }

    /**
     * Elimina (o desactiva) una imagen a partir de su ID.
     * <p>
     * Se asume que solo el usuario que subió la imagen o un administrador puede realizar esta acción.
     * </p>
     *
     * @param id Identificador de la imagen a eliminar.
     * @return ResponseEntity sin contenido, con código HTTP 204.
     */
    @DeleteMapping("/{id}")
    @CheckOwnerOrAdmin(entityClass = Image.class)
     public ResponseEntity<Void> deactivateImage(@PathVariable String id) {
        log.info("Iniciando eliminación de imagen con ID: {}", id);
        imageService.deleteImage(id);
        log.info("Imagen con ID: {} eliminada exitosamente", id);
        return ResponseEntity.noContent().build();
    }


}
