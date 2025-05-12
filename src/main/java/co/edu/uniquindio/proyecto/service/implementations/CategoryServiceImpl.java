package co.edu.uniquindio.proyecto.service.implementations;

import co.edu.uniquindio.proyecto.dto.category.CategoryRequest;
import co.edu.uniquindio.proyecto.dto.category.CategoryResponse;
import co.edu.uniquindio.proyecto.entity.category.Category;
import co.edu.uniquindio.proyecto.exception.category.CategoryNotFoundException;
import co.edu.uniquindio.proyecto.exception.category.DuplicateCategoryException;
import co.edu.uniquindio.proyecto.exception.global.IdInvalidException;
import co.edu.uniquindio.proyecto.repository.CategoryRepository;
import co.edu.uniquindio.proyecto.service.interfaces.CategoryService;
import co.edu.uniquindio.proyecto.service.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación del servicio de comentarios.
 * Proporciona operaciones para crear, consultar, listar y eliminar comentarios asociados a reportes.
 * <p>
 * Funcionalidades:
 * - Crear comentarios y notificar al autor del reporte.
 * - Buscar comentarios por ID.
 * - Listar comentarios de un reporte de forma paginada.
 * - Realizar eliminación lógica (soft delete) de comentarios.
 * <p>
 * Excepciones controladas:
 * - CommentNotFoundException: cuando no se encuentra un comentario.
 * - ReportNotFoundException: cuando no se encuentra el reporte asociado.
 * - IllegalArgumentException: cuando los identificadores tienen formato incorrecto.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    /**
     * Recupera la lista de categorías activas.
     *
     * @return Lista de {@code CategoryResponse} con las categorías activas.
     */
    @Override
    public List<CategoryResponse> findAllActiveCategories() {
        log.info("Solicitando lista de categorías activas");
        return categoryRepository.findAllByActivatedTrue().stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    @Override
    public List<CategoryResponse> findAllCategories() {
        log.info("Solicitando lista de categorías activas");
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    /**
     * Obtiene la información de una categoría a partir de su ID.
     *
     * @param id Identificador de la categoría.
     * @return {@code CategoryResponse} con la información de la categoría.
     * @throws IdInvalidException       Si el ID no es válido.
     * @throws CategoryNotFoundException Si no se encuentra la categoría.
     */
    @Override
    public CategoryResponse getCategoryById(String id) {
        ObjectId objectId = parseObjectId(id);
        log.debug("Buscando categoría con ID: {}", id);
        Category category = categoryRepository.findByIdAndActivatedTrue(objectId)
                .orElseThrow(() -> {
                    log.warn("Categoría no encontrada para ID: {}", id);
                    return new CategoryNotFoundException(id);
                });
        log.info("Categoría con ID: {} encontrada exitosamente", id);
        return categoryMapper.toCategoryResponse(category);
    }

    /**
     * Crea una nueva categoría a partir de los datos del request.
     *
     * @param request Datos para la creación de la categoría.
     * @return {@code CategoryResponse} con la nueva categoría creada.
     * @throws DuplicateCategoryException Si ya existe una categoría con el mismo nombre.
     */
    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        log.debug("Validando existencia de categoría con nombre: {}", request.name());
        if (categoryRepository.existsByName(request.name())) {
            log.warn("Intento de creación de categoría duplicada: {}", request.name());
            throw new DuplicateCategoryException(request.name());
        }
        log.debug("Mapeando datos de request a entidad Category");
        Category newCategory = categoryMapper.toEntity(request);
        Category savedCategory = categoryRepository.save(newCategory);
        log.info("Categoría creada exitosamente con nombre: {}", savedCategory.getName());
        return categoryMapper.toCategoryResponse(savedCategory);
    }

    /**
     * Actualiza los datos de una categoría existente.
     *
     * @param id      Identificador de la categoría a actualizar.
     * @param request Datos de actualización de la categoría.
     * @return {@code CategoryResponse} con la categoría actualizada.
     * @throws IdInvalidException         Si el ID es inválido.
     * @throws CategoryNotFoundException  Si la categoría no se encuentra.
     * @throws DuplicateCategoryException Si se intenta actualizar a un nombre ya existente en otra categoría.
     */
    @Override
    public CategoryResponse updateCategory(String id, CategoryRequest request) {
        ObjectId objectId = parseObjectId(id);
        log.debug("Buscando categoría con ID: {}", id);
        Category existingCategory = categoryRepository.findByIdAndActivatedTrue(objectId)
                .orElseThrow(() -> {
                    log.warn("Categoría no encontrada para ID: {}", id);
                    return new CategoryNotFoundException(id);
                });
        log.debug("Validando unicidad del nombre: {}", request.name());
        if (!existingCategory.getName().equals(request.name()) &&
                categoryRepository.existsByName(request.name())) {
            log.warn("Conflicto de nombre para categoría: {}", request.name());
            throw new DuplicateCategoryException(request.name());
        }
        log.debug("Actualizando datos de la categoría con ID: {}", id);
        categoryMapper.updateCategoryFromRequest(request, existingCategory);
        Category savedCategory = categoryRepository.save(existingCategory);
        log.info("Categoría con ID: {} actualizada exitosamente", id);
        return categoryMapper.toCategoryResponse(savedCategory);
    }

    /**
     * Desactiva una categoría (soft delete) a partir de su ID.
     *
     * @param id Identificador de la categoría a desactivar.
     * @throws IdInvalidException        Si el ID es inválido.
     * @throws CategoryNotFoundException Si la categoría no se encuentra.
     */
    @Override
    public void deactivateCategory(String id) {
        ObjectId objectId = parseObjectId(id);
        log.debug("Buscando categoría con ID: {}", id);
        Category category = categoryRepository.findByIdAndActivatedTrue(objectId)
                .orElseThrow(() -> {
                    log.warn("Categoría no encontrada para ID: {}", id);
                    return new CategoryNotFoundException(id);
                });
        log.debug("Marcando la categoría con ID: {} como inactiva", id);
        category.setActivated(false);
        categoryRepository.save(category);
        log.info("Categoría con ID: {} desactivada exitosamente", id);
    }

    /**
     * Convierte un String a ObjectId.
     *
     * @param id El identificador en formato String.
     * @return El ObjectId correspondiente.
     * @throws IdInvalidException Si el formato del ID es inválido.
     */
    private ObjectId parseObjectId(String id) {
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException e) {
            log.error("ID inválido: {}", id);
            throw new IdInvalidException("ID no válido");
        }
    }
}
