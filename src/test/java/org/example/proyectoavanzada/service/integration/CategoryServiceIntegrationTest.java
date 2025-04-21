package org.example.proyectoavanzada.service.integration;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.dto.category.CategoryRequest;
import co.edu.uniquindio.proyecto.dto.category.CategoryResponse;
import co.edu.uniquindio.proyecto.entity.category.Category;
import co.edu.uniquindio.proyecto.exception.category.CategoryNotFoundException;
import co.edu.uniquindio.proyecto.exception.category.DuplicateCategoryException;
import co.edu.uniquindio.proyecto.exception.global.IdInvalidException;
import co.edu.uniquindio.proyecto.repository.CategoryRepository;
import co.edu.uniquindio.proyecto.service.EmailService;
import co.edu.uniquindio.proyecto.service.interfaces.CategoryService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(classes = ProyectoApplication.class)
public class CategoryServiceIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;
    @MockitoBean
    private EmailService emailService; // Mock para evitar dependencias de correo

    // Dataset con mínimo 5 categorías activas (se simulan 5 "usuarios")
    private List<Category> dataset;

    @BeforeEach
    public void setup() {
        // Limpia la base de datos de pruebas para asegurar la repetibilidad
        categoryRepository.deleteAll();

        // Se crean 5 categorías activas y 1 inactiva (para probar los filtros)
        Category cat1 = createCategory("Cultura", true);
        Category cat2 = createCategory("Tecnología", true);
        Category cat3 = createCategory("Salud", true);
        Category cat4 = createCategory("Educación", true);
        Category cat5 = createCategory("Deportes", true);
        Category cat6 = createCategory("Inactiva", false); // categoría inactiva

        // Se guardan las categorías y se establece el dataset
        dataset = categoryRepository.saveAll(List.of(cat1, cat2, cat3, cat4, cat5, cat6));
    }

    private Category createCategory(String name, boolean activated) {
        Category c = new Category();
        // Se asigna el nombre, descripción y otros datos básicos
        c.setName(name);
        c.setDescription("Descripción de " + name);
        c.setCreatedAt(LocalDateTime.now());
        c.setActivated(activated);
        return c;
    }

    /**
     * Flujo Positivo: Recupera la lista de categorías activas.
     * Se espera que el método retorne 5 categorías activas (excluyendo la inactiva).
     */
    @Test
    @DisplayName("findAllActiveCategories - Retorna lista con categorías activas")
    public void testFindAllActiveCategories_Positive() {
        List<CategoryResponse> responses = categoryService.findAllActiveCategories();
        assertNotNull(responses, "La respuesta no debe ser nula.");
        // Se tienen 5 categorías activas en el dataset.
        assertEquals(5, responses.size(), "Se esperaban 5 categorías activas.");
        // Opcional: verificar algunos nombres
        assertTrue(responses.stream().anyMatch(r -> r.name().equals("Cultura")));
        assertTrue(responses.stream().anyMatch(r -> r.name().equals("Deportes")));
    }

    /**
     * Flujo Positivo: Obtener la información de una categoría activa a partir de su ID.
     */
    @Test
    @DisplayName("getCategoryById - Retorna categoría cuando el ID es válido")
    public void testGetCategoryById_Positive() {
        // Se toma una categoría activa del dataset, por ejemplo la primera que se encuentre activa
        Category activeCategory = dataset.get(0);

        String validId = activeCategory.getId().toHexString();
        CategoryResponse response = categoryService.getCategoryById(validId);
        assertNotNull(response, "El CategoryResponse no debe ser nulo.");
        assertEquals(activeCategory.getName(), response.name(), "El nombre debe coincidir.");
        assertEquals(activeCategory.getDescription(), response.description(), "La descripción debe coincidir.");
    }

    /**
     * Flujo Negativo: Si se proporciona un ID con formato inválido se lanza IdInvalidException.
     */
    @Test
    @DisplayName("getCategoryById - ID inválido lanza IdInvalidException")
    public void testGetCategoryById_InvalidId() {
        String invalidId = "invalid_id";
        assertThrows(IdInvalidException.class, () -> categoryService.getCategoryById(invalidId),
                "Se esperaba lanzar IdInvalidException debido a un ID con formato incorrecto.");
    }

    /**
     * Flujo Negativo: Si el ID corresponde a una categoría inactiva o inexistente se lanza CategoryNotFoundException.
     */
    @Test
    @DisplayName("getCategoryById - Categoría no encontrada/inactiva lanza CategoryNotFoundException")
    public void testGetCategoryById_NotFound() {
        // Utilizamos la categoría inactiva, ya que no está considerada en la búsqueda.
        Category inactiveCategory = dataset.get(5);
        String idInactive = inactiveCategory.getId().toHexString();
        assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategoryById(idInactive),
                "Se esperaba lanzar CategoryNotFoundException para una categoría inactiva.");

        // O también se puede generar un ObjectId válido que no exista en la BD.
        String nonExistentId = new ObjectId().toHexString();
        assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategoryById(nonExistentId),
                "Se esperaba lanzar CategoryNotFoundException para una categoría inexistente.");
    }

    /* ----------------------- PRUEBAS DEL MÉTODO createCategory ----------------------- */

    @Test
    @DisplayName("createCategory - Flujo positivo: Crear categoría nueva")
    public void testCreateCategory_Positive() {
        // Se crea un request con un nombre no existente en el dataset
        CategoryRequest request = new CategoryRequest("Viajes", "Descripción de Viajes");

        // Act: Se invoca el método de creación
        CategoryResponse response = categoryService.createCategory(request);

        // Assert
        assertNotNull(response, "El CategoryResponse no debe ser nulo.");
        assertEquals("Viajes", response.name(), "El nombre debe ser 'Viajes'.");
        // Comprueba que la categoría fue persistida y está activa
        Category created = categoryRepository.findById(new ObjectId(response.id())).orElse(null);
        assertNotNull(created, "La categoría creada debe estar en la base de datos.");
        assertTrue(created.isActivated(), "La categoría creada debe estar activa.");
    }

    @Test
    @DisplayName("createCategory - Flujo negativo: Nombre duplicado lanza DuplicateCategoryException")
    public void testCreateCategory_DuplicateName() {
        // Se utiliza un nombre ya existente, por ejemplo "Cultura" (cat1)
        CategoryRequest request = new CategoryRequest("Cultura", "Otra descripción");

        // Act & Assert: Se espera la excepción de categoría duplicada
        assertThrows(DuplicateCategoryException.class, () -> {
            categoryService.createCategory(request);
        });
    }

    /* ----------------------- PRUEBAS DEL MÉTODO updateCategory ----------------------- */

    @Test
    @DisplayName("updateCategory - Flujo positivo: Actualización exitosa")
    public void testUpdateCategory_Positive() {
        // Seleccionar una categoría activa del dataset, por ejemplo "Salud"
        Category original = dataset.stream().filter(Category::isActivated)
                .filter(c -> c.getName().equals("Salud")).findFirst().orElseThrow();
        String validId = original.getId().toHexString();

        // Se crea un request con un nuevo nombre y descripción, sin duplicar
        CategoryRequest request = new CategoryRequest("Salud y Bienestar", "Actualización en Salud");

        // Act: Se invoca la actualización
        CategoryResponse updatedResponse = categoryService.updateCategory(validId, request);

        // Assert
        assertNotNull(updatedResponse, "El CategoryResponse no debe ser nulo.");
        assertEquals("Salud y Bienestar", updatedResponse.name(), "El nombre debe actualizarse correctamente.");
        // Verifica que en la base de datos se persista el cambio
        Category updatedCategory = categoryRepository.findById(original.getId()).orElseThrow();
        assertEquals("Salud y Bienestar", updatedCategory.getName(), "El nombre en la BD debe reflejar la actualización.");
    }

    @Test
    @DisplayName("updateCategory - Flujo negativo: ID inválido lanza IdInvalidException")
    public void testUpdateCategory_InvalidId() {
        CategoryRequest request = new CategoryRequest("NuevoNombre", "Nueva Descripción");
        // Act & Assert: El ID "invalido" no cumple el formato ObjectId
        assertThrows(IdInvalidException.class, () -> {
            categoryService.updateCategory("id_invalido", request);
        });
    }

    @Test
    @DisplayName("updateCategory - Flujo negativo: Categoría no encontrada lanza CategoryNotFoundException")
    public void testUpdateCategory_NotFound() {
        // Se genera un ObjectId válido que no existe en la BD
        String fakeId = new ObjectId().toHexString();
        CategoryRequest request = new CategoryRequest("NombreCualquiera", "Descripción cualquiera");

        // Act & Assert: Se espera que no se encuentre la categoría y se lance la excepción
        assertThrows(CategoryNotFoundException.class, () -> {
            categoryService.updateCategory(fakeId, request);
        });
    }

    @Test
    @DisplayName("updateCategory - Flujo negativo: Nombre duplicado lanza DuplicateCategoryException")
    public void testUpdateCategory_DuplicateName() {
        // Seleccionar dos categorías activas: usar una para actualizar y otra para generar conflicto
        Category original = dataset.stream().filter(Category::isActivated)
                .filter(c -> c.getName().equals("Educación")).findFirst().orElseThrow();
        Category conflicting = dataset.stream().filter(Category::isActivated)
                .filter(c -> c.getName().equals("Deportes")).findFirst().orElseThrow();

        String validId = original.getId().toHexString();
        // Se crea un request intentando actualizar "Educación" a "Deportes" (ya existente)
        CategoryRequest request = new CategoryRequest(conflicting.getName(), "Nueva descripción");

        // Act & Assert
        assertThrows(DuplicateCategoryException.class, () -> {
            categoryService.updateCategory(validId, request);
        });
    }

    /* ----------------------- PRUEBAS DEL MÉTODO deactivateCategory ----------------------- */

    @Test
    @DisplayName("deactivateCategory - Flujo positivo: Desactivación exitosa")
    public void testDeactivateCategory_Positive() {
        // Seleccionar una categoría activa, por ejemplo "Tecnología"
        Category target = dataset.stream().filter(Category::isActivated)
                .filter(c -> c.getName().equals("Tecnología")).findFirst().orElseThrow();
        String validId = target.getId().toHexString();

        // Act: Se invoca el método de desactivación
        categoryService.deactivateCategory(validId);

        // Assert: La categoría debe quedar desactivada
        Category updated = categoryRepository.findById(target.getId()).orElseThrow();
        assertFalse(updated.isActivated(), "La categoría debe estar marcada como inactiva.");
    }

    @Test
    @DisplayName("deactivateCategory - Flujo negativo: ID inválido lanza IdInvalidException")
    public void testDeactivateCategory_InvalidId() {
        // Act & Assert: Un ID mal formado debe lanzar la excepción
        assertThrows(IdInvalidException.class, () -> {
            categoryService.deactivateCategory("mal_id");
        });
    }

    @Test
    @DisplayName("deactivateCategory - Flujo negativo: Categoría no encontrada lanza CategoryNotFoundException")
    public void testDeactivateCategory_NotFound() {
        // Se genera un ObjectId válido que no existe en la BD
        String fakeId = new ObjectId().toHexString();
        // Act & Assert
        assertThrows(CategoryNotFoundException.class, () -> {
            categoryService.deactivateCategory(fakeId);
        });
    }
}

