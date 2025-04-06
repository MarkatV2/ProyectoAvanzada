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

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ReportExceptionHandler {

    private final ErrorResponseBuilder errorResponseBuilder;

    @ExceptionHandler(DuplicateReportException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateReportException(
            DuplicateReportException ex, WebRequest request
    ) {
        log.error("Reporte Duplicado: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReportNotFound(
            ReportNotFoundException ex, WebRequest request) {
        log.error("Error al encontrar el reporte: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(HistoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleHistoryNotFound(
            HistoryNotFoundException ex, WebRequest request) {
        log.error("Error al encontrar el historial de estados del reporte: {}", ex.getMessage());
        return errorResponseBuilder.buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }


}
