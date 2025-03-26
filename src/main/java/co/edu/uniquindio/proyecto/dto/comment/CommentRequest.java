package co.edu.uniquindio.proyecto.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "El comentario no puede estar vacío")
        @Size(max = 800, message = "El comentario no debe exceder los 800 carácteres")
        String comment
) {}
