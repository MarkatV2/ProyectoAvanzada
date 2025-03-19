package co.edu.uniquindio.proyecto.exception;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(String id) {
        super("Categoría con ID: " + id + " no encontrada");
    }
}
