package co.edu.uniquindio.proyecto.dto.image;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// Request: Solo necesita la URL generada por Cloudinary
public record ImageUploadRequest(
        @NotBlank(message = "La URL de la imagen es obligatoria")
        @Pattern(regexp = "^(http|https)://res.cloudinary.com/.*", message = "URL de Cloudinary inválida")
        String imageUrl
) {}