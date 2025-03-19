package co.edu.uniquindio.proyecto.dto.response;
public record ValidationErrorResponse(
        String field,       // Nombre del campo con error
        String message      // Mensaje de error
) {}
