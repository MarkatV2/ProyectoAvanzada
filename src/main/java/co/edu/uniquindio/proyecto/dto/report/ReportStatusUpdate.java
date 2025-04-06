package co.edu.uniquindio.proyecto.dto.report;

import jakarta.validation.constraints.NotBlank;

public record ReportStatusUpdate(
        @NotBlank(message = "El estado del reporte es obligatorio")
        String status,
        String rejectionMessage
) {
}
