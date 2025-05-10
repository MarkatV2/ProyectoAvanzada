package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.category.CategoryRequest;
import co.edu.uniquindio.proyecto.dto.category.CategoryResponse;
import co.edu.uniquindio.proyecto.service.interfaces.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST para la gestión de categorías.
 * <p>
 * Permite operaciones CRUD sobre categorías por parte de administradores
 * y consulta de categorías activas para todos los usuarios.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Recupera todas las categorías activas disponibles en el sistema.
     *
     * @return Lista de objetos {@link CategoryResponse}.
     */
    @GetMapping
    public List<CategoryResponse> getAllActiveCategories() {
        log.info("📋 Consultando todas las categorías activas");
        return categoryService.findAllActiveCategories();
    }

    /**
     * Obtiene los detalles de una categoría específica por su ID.
     *
     * @param categoryId ID de la categoría.
     * @return Categoría encontrada, con código HTTP 200.
     */
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable String categoryId) {
        log.info("🔍 Buscando categoría con ID: {}", categoryId);
        CategoryResponse categoryResponse = categoryService.getCategoryById(categoryId);
        log.info("✅ Categoría encontrada: {}", categoryId);
        return ResponseEntity.ok(categoryResponse);
    }

    /**
     * Crea una nueva categoría (requiere rol ADMIN).
     * <p>
     * Devuelve la URI del nuevo recurso en la cabecera <code>Location</code>.
     * </p>
     *
     * @param request Datos de la nueva categoría.
     * @return Categoría creada con HTTP 201 y ubicación.
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        log.info("➕ Creando nueva categoría: {}", request.name());
        CategoryResponse createdCategory = categoryService.createCategory(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCategory.id())
                .toUri();

        log.info("✅ Categoría creada con ID: {}", createdCategory.id());
        return ResponseEntity.created(location).body(createdCategory);
    }

    /**
     * Actualiza una categoría existente (requiere rol ADMIN).
     * <p>
     * Devuelve la URI del recurso actualizado en la cabecera <code>Location</code>.
     * </p>
     *
     * @param categoryId ID de la categoría a actualizar.
     * @param request Nuevos datos de la categoría.
     * @return Categoría actualizada con HTTP 200.
     */
    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable String categoryId,
            @Valid @RequestBody CategoryRequest request) {

        log.info("✏️ Actualizando categoría con ID: {}", categoryId);
        CategoryResponse updatedCategory = categoryService.updateCategory(categoryId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .build()
                .toUri();

        log.info("✅ Categoría actualizada con ID: {}", categoryId);
        return ResponseEntity.ok()
                .header(HttpHeaders.LOCATION, location.toString())
                .body(updatedCategory);
    }

    /**
     * Desactiva (elimina lógicamente) una categoría existente (requiere rol ADMIN).
     *
     * @param categoryId ID de la categoría a desactivar.
     * @return HTTP 204 sin contenido.
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deactivateCategory(@PathVariable String categoryId) {
        log.info("⛔ Solicitando desactivación de categoría con ID: {}", categoryId);
        categoryService.deactivateCategory(categoryId);
        log.info("🗑️ Categoría desactivada con éxito: {}", categoryId);
        return ResponseEntity.noContent().build();
    }

}
