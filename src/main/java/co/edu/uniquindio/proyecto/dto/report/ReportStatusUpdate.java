package co.edu.uniquindio.proyecto.dto.report;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para actualizar el estado de un reporte.
 */
public record ReportStatusUpdate(
        @NotBlank(message = "El estado del reporte es obligatorio")
        String status,
        String rejectionMessage
) {
}
