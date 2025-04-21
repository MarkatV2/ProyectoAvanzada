package org.example.proyectoavanzada.controller.unit;

import co.edu.uniquindio.proyecto.controller.CategoryController;
import co.edu.uniquindio.proyecto.dto.category.CategoryRequest;
import co.edu.uniquindio.proyecto.dto.category.CategoryResponse;
import co.edu.uniquindio.proyecto.exception.category.CategoryNotFoundException;
import co.edu.uniquindio.proyecto.exception.category.DuplicateCategoryException;
import co.edu.uniquindio.proyecto.exception.global.IdInvalidException;
import co.edu.uniquindio.proyecto.exceptionhandler.ErrorResponseBuilder;
import co.edu.uniquindio.proyecto.exceptionhandler.category.CategoryExceptionHandler;
import co.edu.uniquindio.proyecto.exceptionhandler.global.GlobalExceptionHandler;
import co.edu.uniquindio.proyecto.service.implementations.CategoryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.example.proyectoavanzada.configuration.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@ContextConfiguration(classes = {CategoryController.class, TestSecurityConfig.class})
@Import({CategoryExceptionHandler.class, ErrorResponseBuilder.class, GlobalExceptionHandler.class})
class CategoryControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryServiceImpl categoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<CategoryResponse> categories;

    @BeforeEach
    void setUp() {
        categories = IntStream.range(1, 9)
                .mapToObj(i -> new CategoryResponse(
                        new ObjectId().toHexString(),
                        "Categoría " + i,
                        "Descripción " + i,
                        LocalDateTime.now().toString()))
                .toList();
    }

    @Test
    void shouldReturnAllActiveCategories() throws Exception {
        // Arrange
        when(categoryService.findAllActiveCategories()).thenReturn(categories);

        // Act & Assert
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(8))
                .andExpect(jsonPath("$[0].name").value("Categoría 1"))
                .andExpect(jsonPath("$[7].name").value("Categoría 8"));

        verify(categoryService).findAllActiveCategories();
    }


    @Test
    void shouldReturnCategoryById() throws Exception {
        // Arrange
        CategoryResponse response = categories.get(0);
        String id = response.id();
        when(categoryService.getCategoryById(id)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/categories/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(response.name()));

        verify(categoryService).getCategoryById(id);
    }

    @Test
    void shouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
        // Arrange
        String id = new ObjectId().toHexString();
        when(categoryService.getCategoryById(id)).thenThrow(new CategoryNotFoundException(id));

        // Act & Assert
        mockMvc.perform(get("/api/v1/categories/" + id))
                .andExpect(status().isNotFound());

        verify(categoryService).getCategoryById(id);
    }

    @Test
    void shouldReturnBadRequestForInvalidObjectId() throws Exception {
        // Arrange
        String invalidId = "invalid-id";
        when(categoryService.getCategoryById(invalidId)).thenThrow(new IdInvalidException(invalidId));

        // Act & Assert
        mockMvc.perform(get("/api/v1/categories/{id}", invalidId))
                .andExpect(status().isBadRequest());

        verify(categoryService).getCategoryById(invalidId);
    }

    // ==================== Tests para createCategory ====================

    @Test
    @DisplayName("Crear categoría - éxito")
    void shouldCreateCategorySuccessfully() throws Exception {
        CategoryRequest req = new CategoryRequest("Nueva Cat", "Desc nueva");
        CategoryResponse created = new CategoryResponse(
                new ObjectId().toHexString(), req.name(), req.description(), LocalDateTime.now().toString());
        when(categoryService.createCategory(req)).thenReturn(created);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION,
                        "http://localhost/api/v1/categories/" + created.id()))
                .andExpect(jsonPath("$.id").value(created.id()));

        verify(categoryService).createCategory(req);
    }

    @Test
    @DisplayName("Crear categoría - nombre duplicado")
    void shouldReturnConflictWhenCreatingDuplicateCategory() throws Exception {
        CategoryRequest req = new CategoryRequest("Categoría 1", "Desc");
        when(categoryService.createCategory(req)).thenThrow(new DuplicateCategoryException(req.name()));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        verify(categoryService).createCategory(req);
    }

    // ==================== Tests para updateCategory ====================

    @Test
    @DisplayName("Actualizar categoría - éxito")
    void shouldUpdateCategorySuccessfully() throws Exception {
        CategoryResponse original = categories.get(0);
        String id = original.id();
        CategoryRequest req = new CategoryRequest("Updated Name", "Updated Desc");
        CategoryResponse updated = new CategoryResponse(id, req.name(), req.description(), original.createdAt());
        when(categoryService.updateCategory(id, req)).thenReturn(updated);

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.LOCATION,
                        "http://localhost/api/v1/categories/" + id))
                .andExpect(jsonPath("$.name").value(req.name()));

        verify(categoryService).updateCategory(id, req);
    }

    @Test
    @DisplayName("Actualizar categoría - no encontrada")
    void shouldReturnNotFoundWhenUpdatingNonExistingCategory() throws Exception {
        String id = new ObjectId().toHexString();
        CategoryRequest req = new CategoryRequest("Name", "Desc");
        when(categoryService.updateCategory(id, req)).thenThrow(new CategoryNotFoundException(id));

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());

        verify(categoryService).updateCategory(id, req);
    }

    @Test
    @DisplayName("Actualizar categoría - nombre duplicado")
    void shouldReturnConflictWhenUpdatingDuplicateName() throws Exception {
        CategoryResponse original = categories.get(0);
        String id = original.id();
        CategoryRequest req = new CategoryRequest("Existing Name", "Desc");
        when(categoryService.updateCategory(id, req)).thenThrow(new DuplicateCategoryException(req.name()));

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        verify(categoryService).updateCategory(id, req);
    }

    @Test
    @DisplayName("Actualizar categoría - ID inválido")
    void shouldReturnBadRequestWhenUpdatingWithInvalidId() throws Exception {
        String invalid = "bad-id";
        CategoryRequest req = new CategoryRequest("Name", "Desc");
        when(categoryService.updateCategory(invalid, req)).thenThrow(new IdInvalidException(invalid));

        mockMvc.perform(put("/api/v1/categories/{id}", invalid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(categoryService).updateCategory(invalid, req);
    }

    // ==================== Tests para deactivateCategory ====================

    @Test
    @DisplayName("Desactivar categoría - éxito")
    void shouldDeactivateCategorySuccessfully() throws Exception {
        String id = categories.get(0).id();
        doNothing().when(categoryService).deactivateCategory(id);

        mockMvc.perform(delete("/api/v1/categories/{id}", id))
                .andExpect(status().isNoContent());

        verify(categoryService).deactivateCategory(id);
    }

    @Test
    @DisplayName("Desactivar categoría - no encontrada")
    void shouldReturnNotFoundWhenDeactivatingNonExistingCategory() throws Exception {
        String id = new ObjectId().toHexString();
        doThrow(new CategoryNotFoundException(id)).when(categoryService).deactivateCategory(id);

        mockMvc.perform(delete("/api/v1/categories/{id}", id))
                .andExpect(status().isNotFound());

        verify(categoryService).deactivateCategory(id);
    }

    @Test
    @DisplayName("Desactivar categoría - ID inválido")
    void shouldReturnBadRequestWhenDeactivatingWithInvalidId() throws Exception {
        String invalid = "123-xyz";
        doThrow(new IdInvalidException(invalid)).when(categoryService).deactivateCategory(invalid);

        mockMvc.perform(delete("/api/v1/categories/{id}", invalid))
                .andExpect(status().isBadRequest());

        verify(categoryService).deactivateCategory(invalid);
    }

}

