package co.edu.uniquindio.proyecto.dto.response;

/**
 * DTO utilizado para devolver un mensaje de éxito simple.
 * Se utiliza generalmente como respuesta a operaciones exitosas sin contenido complejo.
 */
public record SuccessResponse(
        String message
) {}
