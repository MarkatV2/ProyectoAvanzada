package co.edu.uniquindio.proyecto.exception;

// Excepción personalizada
public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException(String id) {
        super("Reporte no encontrado con ID: " + id);
    }
}