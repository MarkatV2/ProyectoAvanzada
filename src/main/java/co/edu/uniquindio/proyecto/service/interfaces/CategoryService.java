package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.dto.category.CategoryRequest;
import co.edu.uniquindio.proyecto.dto.category.CategoryResponse;

import java.util.List;

/**
 * Servicio para gestionar categorías de reportes.
 */
public interface CategoryService {

    /**
     * Obtiene todas las categorías activas.
     *
     * @return lista de categorías activas.
     */
    List<CategoryResponse> findAllActiveCategories();

    /**
     * Crea una nueva categoría.
     *
     * @param request datos de la categoría a crear.
     * @return categoría creada.
     */
    CategoryResponse createCategory(CategoryRequest request);

    /**
     * Actualiza una categoría existente.
     *
     * @param id      ID de la categoría a actualizar.
     * @param request nuevos datos de la categoría.
     * @return categoría actualizada.
     */
    CategoryResponse updateCategory(String id, CategoryRequest request);

    /**
     * Desactiva una categoría (soft delete).
     *
     * @param id ID de la categoría a desactivar.
     */
    void deactivateCategory(String id);

    /**
     * Obtiene una categoría por su ID.
     *
     * @param id ID de la categoría.
     * @return categoría encontrada.
     */
    CategoryResponse getCategoryById(String id);
}
