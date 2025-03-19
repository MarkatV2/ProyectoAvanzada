package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.category.CategoryRequest;
import co.edu.uniquindio.proyecto.dto.category.CategoryResponse;
import co.edu.uniquindio.proyecto.entity.category.Category;
import co.edu.uniquindio.proyecto.exception.CategoryNotFoundException;
import co.edu.uniquindio.proyecto.exception.DuplicateCategoryException;
import co.edu.uniquindio.proyecto.exception.IdInvalidException;
import co.edu.uniquindio.proyecto.repository.CategoryRepository;
import co.edu.uniquindio.proyecto.service.interfaces.CategoryService;
import co.edu.uniquindio.proyecto.service.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImplements implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    //METODO Obtener todas las categorias
    public List<CategoryResponse> findAllActiveCategories() {
        log.info("Solicitando lista de categorias...");
        return categoryRepository.findByActivatedTrue().stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }


    //METODO buscar categoria Id
    public CategoryResponse getCategoryById(String id) {
        ObjectId objectId = parseObjectId(id);

        log.debug("Buscando categoría ID: {}", id);
        Category category = categoryRepository.findByActivatedTrue(objectId)
                .orElseThrow(() -> {
                    log.warn("Categoría no encontrada ID: {}", id);
                    return new CategoryNotFoundException(id);
                });

        log.info("Categoría ID: {} encontrada exitosamente", id);
        return categoryMapper.toCategoryResponse(category);
    }


    //METODO Crear categorias
    public CategoryResponse createCategory(CategoryRequest request) {
        log.debug("Validando existencia de categoría con nombre: {}", request.name());

        if (categoryRepository.existsByName(request.name())) {
            log.warn("Intento de crear categoría duplicada: {}", request.name());
            throw new DuplicateCategoryException("La categoría '" + request.name() + "' ya existe");
        }

        log.debug("Mapeando request a entidad Category");
        Category newCategory = categoryMapper.toEntity(request);

        log.debug("Persistiendo nueva categoría en la base de datos");
        return categoryMapper.toCategoryResponse(categoryRepository.save(newCategory));
    }


    //METODO actualizarCategorias
    public CategoryResponse updateCategory(String id, CategoryRequest request) {
        ObjectId objectId = parseObjectId(id);
        log.debug("Buscando categoría ID: {}", id);
        Category existingCategory = categoryRepository.findByActivatedTrue(objectId)
                .orElseThrow(() -> {
                    log.warn("Categoría no encontrada ID: {}", id);
                    return new CategoryNotFoundException(id);
                });

        log.debug("Validando nombre único para: {}", request.name());
        if (!existingCategory.getName().equals(request.name()) &&
                categoryRepository.existsByName(request.name())) {
            log.warn("Conflicto de nombre: {}", request.name());
            throw new DuplicateCategoryException(request.name());
        }

        log.debug("Actualizando datos de la categoría");
        categoryMapper.updateCategoryFromRequest(request, existingCategory);

        Category savedCategory = categoryRepository.save(existingCategory);
        log.info("Categoría ID: {} persistida exitosamente", id);

        return categoryMapper.toCategoryResponse(savedCategory);
    }


    //SOFT DELETE
    public void deactivateCategory(String id) {
        ObjectId objectId = parseObjectId(id);

        log.debug("Buscando categoría ID: {}", id);
        Category category = categoryRepository.findByActivatedTrue(objectId)
                .orElseThrow(() -> {
                    log.warn("Categoría no encontrada ID: {}", id);
                    return new CategoryNotFoundException(id);
                });

        log.debug("Marcando categoría ID: {} como inactiva", id);
        category.setActivated(false);

        categoryRepository.save(category);
        log.info("Categoría ID: {} desactivada exitosamente", id);
    }

    private ObjectId parseObjectId(String id) {
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException e) {
            log.error("ID inválido: {}", id);
            throw new IdInvalidException("ID no válido");
        }
    }

}