package co.edu.uniquindio.proyecto.DTO;
public record ValidationErrorResponse(
        String field,       // Nombre del campo con error
        String message      // Mensaje de error
) {}
