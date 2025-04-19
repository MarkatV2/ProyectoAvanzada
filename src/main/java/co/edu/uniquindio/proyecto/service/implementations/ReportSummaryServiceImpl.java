package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.report.PaginatedReportSummaryResponse;
import co.edu.uniquindio.proyecto.dto.report.ReportFilterDTO;
import co.edu.uniquindio.proyecto.dto.report.ReportSummaryDTO;
import co.edu.uniquindio.proyecto.entity.category.CategoryRef;
import co.edu.uniquindio.proyecto.entity.report.Report;
import co.edu.uniquindio.proyecto.exception.report.CreatingReportSummaryPdfException;
import co.edu.uniquindio.proyecto.service.interfaces.ReportSummaryService;
import co.edu.uniquindio.proyecto.service.mapper.ReportSummaryMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Implementación del servicio {@link ReportSummaryService} responsable de:
 * <ul>
 *     <li>Filtrar reportes según fechas, categorías y ubicación geográfica.</li>
 *     <li>Transformar entidades {@link Report} en objetos DTO resumidos ({@link ReportSummaryDTO}).</li>
 *     <li>Generar archivos PDF a partir de un resumen de reportes usando una plantilla HTML.</li>
 * </ul>
 *
 * Utiliza {@link MongoTemplate} para ejecutar consultas personalizadas sobre la base de datos MongoDB.
 * <p>
 * El archivo de plantilla HTML debe encontrarse en <code>resources/templates/report-summary-template.html</code>
 * y debe incluir el marcador <code>{{rows}}</code>, que será reemplazado por el contenido generado dinámicamente.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportSummaryServiceImpl implements ReportSummaryService {

    private final MongoTemplate mongoTemplate;
    private final ReportSummaryMapper reportSummaryMapper;


    /**
     * Obtiene una lista de reportes filtrados según los criterios especificados.
     *
     * @param filter Objeto que contiene los filtros: fechas, categorías y ubicación.
     * @return Lista de resúmenes de reportes que cumplen los criterios.
     * @throws IllegalArgumentException si las fechas son inválidas.
     */
    @Override
    public PaginatedReportSummaryResponse getFilteredReports(ReportFilterDTO filter, int page, int size) {

        Criteria criteria = buildCriteria(filter);
        Query query = new Query(criteria);

        long total = mongoTemplate.count(query, Report.class); // <-- importante
        query.skip((long) (page - 1) * size).limit(size);       // <-- paginación real

        log.debug("Ejecutando query paginada: {}", query);
        List<Report> reports = mongoTemplate.find(query, Report.class);
        log.info("Reportes encontrados (pagina): {}", reports.size());

        List<ReportSummaryDTO> content = reportSummaryMapper.toReportSummaryDto(reports);

        return new PaginatedReportSummaryResponse(content, page, size, total,
                (int) Math.ceil((double) total / size));
    }

    /**
     * Genera un archivo PDF a partir de una lista de reportes resumidos.
     *
     * @param paginated Lista de reportes a incluir en el PDF.
     * @return Arreglo de bytes que representa el contenido del PDF generado.
     * @throws RuntimeException si ocurre un error durante la generación del PDF.
     */
    @Override
    public byte[] generatePdf(PaginatedReportSummaryResponse paginated) {
        log.info("Iniciando generación de PDF para {} reportes (página {}/{})",
                paginated.content().size(), paginated.page(), paginated.totalPages());

        String html = buildHtmlTable(paginated);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();

            log.info("PDF generado exitosamente. Tamaño en bytes: {}", outputStream.size());
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Error generando el PDF", e);
            throw new CreatingReportSummaryPdfException(e.getMessage());
        }
    }


    /**
     * Construye el objeto Criteria para la consulta basada en el filtro.
     *
     * @param filter Filtro con criterios de búsqueda.
     * @return Criteria con condiciones aplicadas.
     */
    private Criteria buildCriteria(ReportFilterDTO filter) {
        Criteria criteria = new Criteria();

        // Filtrar por fecha solo si hay alguna fecha presente
        if (filter.startDate() != null && filter.endDate() != null) {
            criteria = Criteria.where("createdAt")
                    .gte(filter.startDate())
                    .lte(filter.endDate());
        } else if (filter.startDate() != null) {
            criteria = Criteria.where("createdAt")
                    .gte(filter.startDate());
        } else if (filter.endDate() != null) {
            criteria = Criteria.where("createdAt")
                    .lte(filter.endDate());
        }

        applyCategoryFilter(criteria, filter);
        applyLocationFilter(criteria, filter);

        return criteria;
    }


    /**
     * Aplica el filtro de categorías al Criteria si hay categorías definidas.
     *
     * @param criteria Criteria actual a modificar.
     * @param filter   Filtro con lista de IDs de categoría.
     */
    private void applyCategoryFilter(Criteria criteria, ReportFilterDTO filter) {
        if (filter.categoryIds() != null && !filter.categoryIds().isEmpty()) {
            log.info("Filtrando por categorías: {}", filter.categoryIds());
            criteria.and("categoryList.id").in(
                    filter.categoryIds()
            );
        }
    }


    /**
     * Aplica el filtro geográfico al Criteria si se define centro y radio.
     *
     * @param criteria Criteria actual a modificar.
     * @param filter   Filtro con información geográfica.
     */
    private void applyLocationFilter(Criteria criteria, ReportFilterDTO filter) {
        if (filter.center() != null && filter.radiusKm() > 0) {
            log.info("Filtrando por ubicación: centro={} radio={} km", filter.center(), filter.radiusKm());
            criteria.and("location")
                    .nearSphere(filter.center())
                    .maxDistance(kmToRadians(filter.radiusKm()));
        }
    }

    /**
     * Convierte kilómetros a radianes para consultas geoespaciales.
     *
     * @param km Distancia en kilómetros.
     * @return Distancia en radianes.
     */
    private double kmToRadians(double km) {
        return km / 6371.0;
    }



    /**
     * Construye una representación HTML de una tabla con los datos proporcionados en la lista de reportes.
     * Usa una plantilla HTML que contiene un marcador {{rows}}, el cual es reemplazado por las filas generadas dinámicamente.
     *
     * @param paginated Lista de objetos {@link ReportSummaryDTO} que representan los reportes a mostrar en la tabla.
     * @return Cadena de texto HTML con la tabla completamente generada y embebida en la plantilla.
     */
    private String buildHtmlTable(PaginatedReportSummaryResponse paginated) {
        String template = loadHtmlTemplate(); // sigue siendo tu base

        StringBuilder rowsBuilder = new StringBuilder();
        for (ReportSummaryDTO r : paginated.content()) {
            rowsBuilder.append("<tr>")
                    .append("<td>").append(escapeHtml(r.reportId())).append("</td>")
                    .append("<td>").append(escapeHtml(r.title())).append("</td>")
                    .append("<td>").append(escapeHtml(r.description())).append("</td>")
                    .append("<td>").append(escapeHtml(String.join(", ", r.categoryNames()))).append("</td>")
                    .append("<td>").append(escapeHtml(r.status())).append("</td>")
                    .append("<td>").append(r.createdAt()).append("</td>")
                    .append("<td>").append(r.latitude()).append("</td>")
                    .append("<td>").append(r.longitude()).append("</td>")
                    .append("</tr>");
        }

        String metadata = String.format(
                "<p><strong>Total de reportes:</strong> %d<br />" +
                        "<strong>Página:</strong> %d<br />" +
                        "<strong>Total páginas:</strong> %d<br />" +
                        "<strong>Tamaño de página:</strong> %d </p>",
                paginated.totalElements(), paginated.page(), paginated.totalPages(), paginated.size()
        );

        return template
                .replace("{{metadata}}", metadata)
                .replace("{{rows}}", rowsBuilder.toString());
    }



    /**
     * Escapa caracteres especiales en una cadena de texto para que sean seguros en HTML.
     * Esto previene inyecciones HTML al reemplazar caracteres como <, >, &, ", ' por sus equivalentes seguros.
     *
     * @param input Texto a escapar.
     * @return Cadena escapada lista para ser insertada en un documento HTML.
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }


    /**
     * Carga el contenido de una plantilla HTML ubicada en el classpath, específicamente en
     * "templates/report-summary-template.html". Esta plantilla debe contener el marcador {{rows}}
     * que será reemplazado por el contenido dinámico de la tabla.
     *
     * @return Contenido de la plantilla HTML como una cadena de texto.
     * @throws IllegalArgumentException si la plantilla no se encuentra.
     * @throws UncheckedIOException si ocurre un error al leer el archivo.
     */
    private String loadHtmlTemplate() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("templates/report-summary-template.html")) {
            if (is == null) {
                throw new IllegalArgumentException("Plantilla no encontrada: templates/report-summary-template.html");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Error cargando el HTML", e);
        }
    }

}
