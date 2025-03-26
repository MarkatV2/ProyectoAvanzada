package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.category.CategoryResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageResponse;
import co.edu.uniquindio.proyecto.dto.image.ImageUploadRequest;
import co.edu.uniquindio.proyecto.service.implementations.ImageServiceImplements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/images")
public class ImageController {

    private final ImageServiceImplements imageService;

    //METODO para obtener images por su ID
    @GetMapping("/{imageId}") //FALTA TODOS LOS HANDLER DE IMAGENES
    public ResponseEntity<ImageResponse> getImageById (@PathVariable String imageId){
        log.info("Iniciando búsqueda de Imagen ID: {}", imageId);

        ImageResponse imageResponse = imageService.getImageById(imageId);

        log.info("Imagen ID: {} encontrada exitosamente", imageId);
        return ResponseEntity.ok(imageResponse); // 200 OK
    }


    @PostMapping
    public ResponseEntity<ImageResponse> registerImage(
            @Valid @RequestBody ImageUploadRequest request) {

        log.info("Registrando nueva imagen desde URL: {}", request.imageUrl());
        ImageResponse savedImage = imageService.registerImage(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedImage.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(savedImage);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateImage(@PathVariable String id) {
        log.info("Iniciando eliminación de imagen ID: {}", id);

        imageService.deleteImage(id);

        log.info("Imagen ID: {} eliminada exitosamente", id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

}
