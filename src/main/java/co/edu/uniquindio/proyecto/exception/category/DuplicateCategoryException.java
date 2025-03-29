package co.edu.uniquindio.proyecto.exception.category;

public class DuplicateCategoryException extends RuntimeException {
    public DuplicateCategoryException(String message) {
        super("la categoria \"" + message + "\" ya existe!!" );
    }
}