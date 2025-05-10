package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.dto.report.PaginatedReportSummaryResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportFilterDTO;
import org.springframework.security.access.prepost.PreAuthorize;


/**
 * Servicio para generar resúmenes y reportes en PDF de los incidentes.
 */

@PreAuthorize("hasRole('ADMIN')")
public interface ReportSummaryService {

    /**
     * Obtiene los reportes filtrados según los criterios dados.
     *
     * @param filter criterios de filtrado.
     * @return lista de reportes filtrados.
     */
    PaginatedReportSummaryResponse getFilteredReports(ReportFilterDTO filter, int page, int size);

    /**
     * Genera un archivo PDF con el resumen de los reportes.
     *
     * @param paginated lista de reportes a incluir en el PDF.
     * @return contenido del PDF en bytes.
     */
    byte[] generatePdf(PaginatedReportSummaryResponse paginated);
}

