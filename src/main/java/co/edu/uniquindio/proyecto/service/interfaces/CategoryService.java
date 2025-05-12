package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.dto.category.CategoryRequest;
import co.edu.uniquindio.proyecto.dto.category.CategoryResponse;
import org.springframework.security.access.prepost.PreAuthorize;

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
    @PreAuthorize("hasRole('ADMIN')")
    CategoryResponse createCategory(CategoryRequest request);

    /**
     * Actualiza una categoría existente.
     *
     * @param id      ID de la categoría a actualizar.
     * @param request nuevos datos de la categoría.
     * @return categoría actualizada.
     */
    @PreAuthorize("hasRole('ADMIN')")
    CategoryResponse updateCategory(String id, CategoryRequest request);

    /**
     * Desactiva una categoría (soft delete).
     *
     * @param id ID de la categoría a desactivar.
     */
    @PreAuthorize("hasRole('ADMIN')")
    void deactivateCategory(String id);

    @PreAuthorize("hasRole('ADMIN')")
    List<CategoryResponse> findAllCategories();

    CategoryResponse getCategoryById(String id);
}
