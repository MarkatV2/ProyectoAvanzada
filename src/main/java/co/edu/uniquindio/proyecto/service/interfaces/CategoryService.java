package co.edu.uniquindio.proyecto.service.interfaces;

import co.edu.uniquindio.proyecto.dto.category.CategoryRequest;
import co.edu.uniquindio.proyecto.dto.category.CategoryResponse;

import java.util.List;

public interface CategoryService {
    public List<CategoryResponse> findAllActiveCategories();
    public CategoryResponse createCategory(CategoryRequest request);
    public CategoryResponse updateCategory(String id, CategoryRequest request);
    public void deactivateCategory(String id);
    public CategoryResponse getCategoryById(String id);
}
