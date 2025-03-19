package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.category.CategoryRequest;
import co.edu.uniquindio.proyecto.dto.category.CategoryResponse;
import co.edu.uniquindio.proyecto.entity.category.Category;
import co.edu.uniquindio.proyecto.service.implementations.CategoryServiceImplements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
@Slf4j
public class CategoryController {

    private final CategoryServiceImplements categoryService;
    @GetMapping
    public List<CategoryResponse> getAllActiveCategories() {
        log.info("Consultando Categorias...");
        return categoryService.findAllActiveCategories();
    }


    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable String id) {
        log.info("Iniciando búsqueda de categoría ID: {}", id);

        CategoryResponse categoryResponse = categoryService.getCategoryById(id);

        log.info("Categoría ID: {} encontrada exitosamente", id);
        return ResponseEntity.ok(categoryResponse); // 200 OK
    }


    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request
    ) {
        log.info("Iniciando creación de categoría: {}", request.name());
        CategoryResponse createdCategory = categoryService.createCategory(request);

        log.info("Categoría creada exitosamente con ID: {}", createdCategory.id());
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCategory.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(createdCategory);
    }


    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody CategoryRequest request
    ) {
        log.info("Iniciando actualización de categoría ID: {}", id);

        CategoryResponse updatedCategory = categoryService.updateCategory(id, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .build()
                .toUri();

        log.info("Categoría ID: {} actualizada exitosamente", id);

        return ResponseEntity.ok()
                .header(HttpHeaders.LOCATION, location.toString())
                .body(updatedCategory);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateCategory(@PathVariable String id) {
        log.info("Iniciando desactivación de categoría ID: {}", id);

        categoryService.deactivateCategory(id);

        log.info("Categoría ID: {} desactivada exitosamente", id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

}

