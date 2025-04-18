package co.edu.uniquindio.proyecto.exception.report;

public class CreatingReportSummaryPdfException extends RuntimeException{
    public CreatingReportSummaryPdfException(String message){
        super("Error creando el informe en pdf: " + message  );
    }
}
