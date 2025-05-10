package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageUploadRequest;
import co.edu.uniquindio.proyecto.service.interfaces.ImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Controlador REST para la gestión de imágenes.
 * <p>
 * Permite operaciones de consulta, registro y eliminación lógica de imágenes.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final ImageService imageService;

    /**
     * Consulta los detalles de una imagen específica mediante su ID.
     *
     * @param imageId ID de la imagen.
     * @return Objeto {@link ImageResponse} con la información de la imagen.
     */
    @GetMapping("/{imageId}")
    public ResponseEntity<ImageResponse> getImageById(@PathVariable String imageId) {
        log.info("🔍 Consultando imagen con ID: {}", imageId);
        ImageResponse imageResponse = imageService.getImageById(imageId);
        log.info("✅ Imagen encontrada: {}", imageId);
        return ResponseEntity.ok(imageResponse);
    }

    /**
     * Registra una nueva imagen en el sistema.
     * <p>
     * La imagen se almacena junto con metadatos relevantes.
     * </p>
     *
     * @param request Objeto con los datos necesarios para el registro de la imagen.
     * @return Objeto {@link ImageResponse} con la información de la imagen creada y su ubicación.
     */
    @PostMapping
    public ResponseEntity<ImageResponse> registerImage(@Valid @RequestBody ImageUploadRequest request) {
        log.info("🖼️ Registrando imagen desde URL: {}", request.imageUrl());
        ImageResponse savedImage = imageService.registerImage(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedImage.id())
                .toUri();

        log.info("✅ Imagen registrada exitosamente con ID: {}", savedImage.id());
        return ResponseEntity.created(location).body(savedImage);
    }

    /**
     * Elimina lógicamente una imagen (soft delete).
     * <p>
     * Solo el propietario o un administrador puede ejecutar esta acción.
     * </p>
     *
     * @param id ID de la imagen a eliminar.
     * @return HTTP 204 sin contenido si la operación es exitosa.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateImage(@PathVariable String id) {
        log.info("🗑️ Solicitando eliminación de imagen con ID: {}", id);
        imageService.deleteImage(id);
        log.info("✅ Imagen con ID: {} eliminada correctamente", id);
        return ResponseEntity.noContent().build();
    }
}