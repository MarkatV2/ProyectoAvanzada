package co.edu.uniquindio.proyecto.dto.image;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO utilizado para solicitar el registro de una imagen.
 * Incluye la URL de la imagen y su relación con un reporte y un usuario.
 */
public record ImageUploadRequest(
        @NotBlank(message = "La URL de la imagen es obligatoria")
        @Pattern(regexp = "^(http|https)://res.cloudinary.com/.*", message = "URL de Cloudinary inválida")
        String imageUrl,
        @NotBlank(message = "El id del reporte es obligatorio")
        String reportId
) {}