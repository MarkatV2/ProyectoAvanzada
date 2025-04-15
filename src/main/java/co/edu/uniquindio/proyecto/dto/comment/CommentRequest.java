package co.edu.uniquindio.proyecto.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO utilizado para enviar un nuevo comentario desde el cliente.
 * Incluye el texto del comentario y el ID del reporte asociado.
 */
public record CommentRequest(
        @NotBlank(message = "El comentario no puede estar vacío")
        @Size(max = 800, message = "El comentario no debe exceder los 800 carácteres")
        String comment,

        @NotBlank(message = "El id del reporte es obligatorio")
        String reportId
) {}
