package co.edu.uniquindio.proyecto.exceptionhandler.report;

import co.edu.uniquindio.proyecto.dto.response.ErrorResponse;
import co.edu.uniquindio.proyecto.exception.report.DuplicateReportException;
import co.edu.uniquindio.proyecto.exception.report.HistoryNotFoundException;
import co.edu.uniquindio.proyecto.exception.report.ReportNotFoundException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Manejador global de excepciones relacionadas con los reportes de incidentes.
 * <p>
 * Este manejador captura excepciones del dominio de reportes y genera respuestas
 * estandarizadas mediante {@link ErrorResponseBuilder}.
 * </p>
 *
 * <p>
 * Maneja errores comunes como duplicidad de reportes, reportes no encontrados,
 * e historial de estados ausente.
 * </p>
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ReportExceptionHandler {

    private final ErrorResponseBuilder errorResponseBuilder;

    /**
     * Maneja la excepción {@link DuplicateReportException} cuando se intenta registrar
     * un reporte que ya existe.
     *
     * @param ex      Excepción lanzada.
     * @param request Contexto de la petición.
     * @return {@link ResponseEntity} con código 409 (CONFLICT).
     */
    @ExceptionHandler(DuplicateReportException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateReportException(
            DuplicateReportException ex, WebRequest request) {
        log.warn("Reporte duplicado: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    /**
     * Maneja la excepción {@link ReportNotFoundException} cuando no se encuentra
     * un reporte con el identificador solicitado.
     *
     * @param ex      Excepción lanzada.
     * @param request Contexto de la petición.
     * @return {@link ResponseEntity} con código 404 (NOT FOUND).
     */
    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReportNotFound(
            ReportNotFoundException ex, WebRequest request) {
        log.warn("Reporte no encontrado: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    /**
     * Maneja la excepción {@link HistoryNotFoundException} cuando no se encuentra
     * el historial de estados para un reporte dado.
     *
     * @param ex      Excepción lanzada.
     * @param request Contexto de la petición.
     * @return {@link ResponseEntity} con código 404 (NOT FOUND).
     */
    @ExceptionHandler(HistoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleHistoryNotFound(
            HistoryNotFoundException ex, WebRequest request) {
        log.warn("Historial de estados no encontrado: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }
}
