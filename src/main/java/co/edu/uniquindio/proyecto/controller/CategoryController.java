package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.category.CategoryRequest;
import co.edu.uniquindio.proyecto.dto.category.CategoryResponse;
import co.edu.uniquindio.proyecto.service.implementations.CategoryServiceImplements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador para la gestión de categorías.
 * <p>
 * Proporciona endpoints para obtener, crear, actualizar y desactivar categorías.
 * </p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
@Slf4j
public class CategoryController {

    private final CategoryServiceImplements categoryService;

    /**
     * Recupera la lista de categorías activas.
     *
     * @return Lista de {@code CategoryResponse}.
     */
    @GetMapping
    public List<CategoryResponse> getAllActiveCategories() {
        log.info("Consultando categorías activas");
        return categoryService.findAllActiveCategories();
    }

    /**
     * Obtiene la información de una categoría a partir de su ID.
     *
     * @param id Identificador de la categoría.
     * @return ResponseEntity con la categoría encontrada y código HTTP 200.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable String id) {
        log.info("Iniciando búsqueda de categoría con ID: {}", id);
        CategoryResponse categoryResponse = categoryService.getCategoryById(id);
        log.info("Categoría con ID: {} encontrada exitosamente", id);
        return ResponseEntity.ok(categoryResponse);
    }

    /**
     * Crea una nueva categoría a partir de la información proporcionada. (solo Admins)
     *
     * @param request Datos para la creación de la categoría.
     * @return ResponseEntity con la categoría creada y la URI del recurso.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        log.info("Iniciando creación de categoría: {}", request.name());
        CategoryResponse createdCategory = categoryService.createCategory(request);
        log.info("Categoría creada exitosamente con ID: {}", createdCategory.id());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCategory.id())
                .toUri();

        return ResponseEntity.created(location).body(createdCategory);
    }

    /**
     * Actualiza la información de una categoría existente. (solo Admins)
     *
     * @param id      Identificador de la categoría a actualizar.
     * @param request Datos de actualización.
     * @return ResponseEntity con la categoría actualizada y la URI del recurso.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody CategoryRequest request) {
        log.info("Iniciando actualización de categoría con ID: {}", id);
        CategoryResponse updatedCategory = categoryService.updateCategory(id, request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .build()
                .toUri();
        log.info("Categoría con ID: {} actualizada exitosamente", id);
        return ResponseEntity.ok()
                .header(HttpHeaders.LOCATION, location.toString())
                .body(updatedCategory);
    }

    /**
     * Desactiva (soft delete) una categoría a partir de su ID. (solo Admins)
     *
     * @param id Identificador de la categoría a desactivar.
     * @return ResponseEntity con código HTTP 204 (No Content).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateCategory(@PathVariable String id) {
        log.info("Iniciando desactivación de categoría con ID: {}", id);
        categoryService.deactivateCategory(id);
        log.info("Categoría con ID: {} desactivada exitosamente", id);
        return ResponseEntity.noContent().build();
    }

}
