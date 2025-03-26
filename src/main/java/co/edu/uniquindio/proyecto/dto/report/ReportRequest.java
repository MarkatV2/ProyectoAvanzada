package co.edu.uniquindio.proyecto.dto.report;

import co.edu.uniquindio.proyecto.entity.category.CategoryRef;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReportRequest (
        @NotBlank(message = "El titulo es obligatorio")
        String title,
        @NotBlank(message = "El reporte debe contener una descripción")
        String description,
        @NotNull(message = "El reporte debe contener al menos una categoría")
        List<CategoryRef> categoryList,
        @NotNull(message = "La latitud de la dirección es obligatoria")
        double latitude,
        @NotNull(message = "La longitud de la dirección es obligatoria")
        double longitude
) {

}
