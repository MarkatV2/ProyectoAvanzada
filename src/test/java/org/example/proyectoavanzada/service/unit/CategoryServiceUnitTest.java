package org.example.proyectoavanzada.service.unit;

import co.edu.uniquindio.proyecto.dto.category.CategoryRequest;
import co.edu.uniquindio.proyecto.dto.category.CategoryResponse;
import co.edu.uniquindio.proyecto.entity.category.Category;
import co.edu.uniquindio.proyecto.exception.category.CategoryNotFoundException;
import co.edu.uniquindio.proyecto.exception.category.DuplicateCategoryException;
import co.edu.uniquindio.proyecto.exception.global.IdInvalidException;
import co.edu.uniquindio.proyecto.repository.CategoryRepository;
import co.edu.uniquindio.proyecto.service.implementations.CategoryServiceImpl;
import co.edu.uniquindio.proyecto.service.mapper.CategoryMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceUnitTest {

    // Simula la capa de persistencia
    @Mock
    private CategoryRepository categoryRepository;
    // Simula la conversión de entidad a DTO (por ejemplo, usando MapStruct)
    @Mock
    private CategoryMapper categoryMapper;
    // Inyección de los mocks en la implementación real del servicio
    @InjectMocks
    private CategoryServiceImpl categoryService;

    // Definición de 5 categorías activas (simulando "usuarios" o registros de datos)
    private Category category1;
    private Category category2;
    private Category category3;
    private Category category4;
    private Category category5;

    private CategoryResponse response1;
    private CategoryResponse response2;
    private CategoryResponse response3;
    private CategoryResponse response4;
    private CategoryResponse response5;

    /**
     * Método que se ejecuta antes de cada prueba para inicializar datos y configurar el escenario de prueba.
     */
    @BeforeEach
    public void setUp() {
        // Arrange: Inicialización de categorías activas
        category1 = createActiveCategory("Cultura");
        category2 = createActiveCategory("Tecnología");
        category3 = createActiveCategory("Salud");
        category4 = createActiveCategory("Educación");
        category5 = createActiveCategory("Deportes");

        // Creación de respuestas correspondientes (DTO)
        response1 = createCategoryResponse(category1);
        response2 = createCategoryResponse(category2);
        response3 = createCategoryResponse(category3);
        response4 = createCategoryResponse(category4);
        response5 = createCategoryResponse(category5);
    }

    // Método auxiliar para crear una entidad Category activa.
    private Category createActiveCategory(String name) {
        Category c = new Category();
        c.setId(new ObjectId());
        c.setName(name);
        c.setDescription("Descripción de " + name);
        c.setCreatedAt(LocalDateTime.now());
        c.setActivated(true);
        return c;
    }

    /**
     * Caso positivo: Se verifica que el servicio retorne una lista de 5 categorías activas.
     */
    @Test
    @DisplayName("findAllActiveCategories: Retorna lista con 5 categorías activas")
    public void testFindAllActiveCategories_Positive() {
        // Arrange: Se simula que el repositorio retorna 5 categorías activas.
        when(categoryRepository.findAllByActivatedTrue())
                .thenReturn(List.of(category1, category2, category3, category4, category5));

        // Se simulan las conversiones de cada entidad a DTO.
        when(categoryMapper.toCategoryResponse(category1)).thenReturn(response1);
        when(categoryMapper.toCategoryResponse(category2)).thenReturn(response2);
        when(categoryMapper.toCategoryResponse(category3)).thenReturn(response3);
        when(categoryMapper.toCategoryResponse(category4)).thenReturn(response4);
        when(categoryMapper.toCategoryResponse(category5)).thenReturn(response5);

        // Act: Se invoca el método a probar.
        List<CategoryResponse> result = categoryService.findAllActiveCategories();

        // Assert: Se verifica que se hayan obtenido 5 respuestas y se validan algunos valores.
        assertNotNull(result, "La lista de respuestas no debe ser nula.");
        assertEquals(5, result.size(), "Se esperaban 5 categorías activas.");
        assertEquals("Cultura", result.get(0).name(), "El primer elemento debe tener el nombre 'Cultura'.");
        assertEquals("Deportes", result.get(4).name(), "El último elemento debe tener el nombre 'Deportes'.");

        // Verifica la invocación de los métodos simulados
        verify(categoryRepository, times(1)).findAllByActivatedTrue();
        verify(categoryMapper, times(5)).toCategoryResponse(any(Category.class));
    }

    /**
     * Caso alternativo: Cuando no hay categorías activas, se espera obtener una lista vacía.
     */
    @Test
    @DisplayName("findAllActiveCategories: Retorna lista vacía si no hay categorías activas")
    public void testFindAllActiveCategories_EmptyList() {
        // Arrange: El repositorio retorna una lista vacía.
        when(categoryRepository.findAllByActivatedTrue()).thenReturn(List.of());

        // Act: Se invoca el método a probar.
        List<CategoryResponse> result = categoryService.findAllActiveCategories();

        // Assert: La lista no es nula pero está vacía.
        assertNotNull(result, "La respuesta no debe ser nula.");
        assertTrue(result.isEmpty(), "La lista debe estar vacía.");

        verify(categoryRepository, times(1)).findAllByActivatedTrue();
        verify(categoryMapper, never()).toCategoryResponse(any());
    }

    /**
     * Caso de error: Se simula que ocurre un error en la capa de persistencia y se verifica que la excepción se propague.
     */
    @Test
    @DisplayName("findAllActiveCategories: Lanza excepción si hay error en la consulta del repositorio")
    public void testFindAllActiveCategories_RepositoryError() {
        // Arrange: El repositorio lanza una excepción al intentar consultar.
        when(categoryRepository.findAllByActivatedTrue())
                .thenThrow(new RuntimeException("Error de conexión a MongoDB"));

        // Act & Assert: Se verifica que se lance la excepción con el mensaje esperado.
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                categoryService.findAllActiveCategories(), "Debe lanzarse una excepción por error en el repositorio.");

        assertEquals("Error de conexión a MongoDB", exception.getMessage(), "El mensaje de error no coincide.");

        verify(categoryRepository, times(1)).findAllByActivatedTrue();
        verify(categoryMapper, never()).toCategoryResponse(any());
    }


    /**
     * Caso positivo: Se obtiene la información de una categoría a partir de un ID válido.
     */
    @Test
    @DisplayName("getCategoryById - Flujo positivo")
    public void testGetCategoryById_Positive() {
        // Arrange: Se toma category1 para la prueba
        String validId = category1.getId().toHexString();

        when(categoryRepository.findByIdAndActivatedTrue(category1.getId()))
                .thenReturn(Optional.of(category1));
        CategoryResponse response = createCategoryResponse(category1);
        when(categoryMapper.toCategoryResponse(category1))
                .thenReturn(response);

        // Act: Se invoca el método a probar
        CategoryResponse result = categoryService.getCategoryById(validId);

        // Assert: Verificar que se obtiene el CategoryResponse esperado
        assertNotNull(result, "El resultado no debe ser nulo.");
        assertEquals(category1.getName(), result.name(), "El nombre de la categoría debe coincidir.");
        verify(categoryRepository, times(1)).findByIdAndActivatedTrue(category1.getId());
        verify(categoryMapper, times(1)).toCategoryResponse(category1);
    }

    /**
     * Caso de error: Se lanza IdInvalidException cuando el ID no es válido.
     */
    @Test
    @DisplayName("getCategoryById - Id inválido lanza IdInvalidException")
    public void testGetCategoryById_InvalidId() {
        // Arrange: Se utiliza un ID no válido (formato incorrecto para ObjectId)
        String invalidId = "1234";

        // Act & Assert: Se espera que se lance la excepción IdInvalidException
        assertThrows(IdInvalidException.class, () -> categoryService.getCategoryById(invalidId));
        verify(categoryRepository, never()).findByIdAndActivatedTrue(any());
        verify(categoryMapper, never()).toCategoryResponse(any());
    }

    /**
     * Caso de error: Se lanza CategoryNotFoundException cuando no se encuentra la categoría activa.
     */
    @Test
    @DisplayName("getCategoryById - Categoría no encontrada lanza CategoryNotFoundException")
    public void testGetCategoryById_NotFound() {
        // Arrange: Se utiliza un ID válido, pero la consulta en el repositorio retorna vacío
        String validId = category1.getId().toHexString();

        when(categoryRepository.findByIdAndActivatedTrue(category1.getId()))
                .thenReturn(Optional.empty());

        // Act & Assert: Se espera la excepción CategoryNotFoundException
        assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategoryById(validId));
        verify(categoryRepository, times(1)).findByIdAndActivatedTrue(category1.getId());
        verify(categoryMapper, never()).toCategoryResponse(any());
    }

    private CategoryResponse createCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId().toHexString(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt().toString()
        );
    }

    /**
     * Caso positivo: Crear una nueva categoría cuando su nombre no existe en la base de datos.
     */
    @Test
    @DisplayName("createCategory - Flujo positivo, nueva categoría creada exitosamente")
    public void testCreateCategory_Positive() {
        // Arrange
        // Nombre de categoría nuevo (no duplicado)
        CategoryRequest request = new CategoryRequest("Innovación", "Descripción de Innovación");

        // Simular que el repositorio indica que no existe la categoría con ese nombre.
        when(categoryRepository.existsByName(request.name())).thenReturn(false);

        // Simular el mapeo de request a entidad
        Category newCategory = createActiveCategory(request.name());
        when(categoryMapper.toEntity(request)).thenReturn(newCategory);

        // Simular la operación de guardado en el repositorio.
        Category savedCategory = new Category();
        savedCategory.setId(new ObjectId());
        savedCategory.setName(newCategory.getName());
        savedCategory.setDescription(newCategory.getDescription());
        savedCategory.setCreatedAt(LocalDateTime.now());
        savedCategory.setActivated(true);
        when(categoryRepository.save(newCategory)).thenReturn(savedCategory);

        // Simular el mapeo de la entidad guardada a CategoryResponse.
        CategoryResponse response = new CategoryResponse(
                savedCategory.getId().toHexString(),
                savedCategory.getName(),
                savedCategory.getDescription(),
                savedCategory.getCreatedAt().toString()
        );
        when(categoryMapper.toCategoryResponse(savedCategory)).thenReturn(response);

        // Act
        CategoryResponse result = categoryService.createCategory(request);

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo.");
        assertEquals(request.name(), result.name(), "El nombre de la categoría debe coincidir.");
        verify(categoryRepository, times(1)).existsByName(request.name());
        verify(categoryMapper, times(1)).toEntity(request);
        verify(categoryRepository, times(1)).save(newCategory);
        verify(categoryMapper, times(1)).toCategoryResponse(savedCategory);
    }

    /**
     * Caso de error: Se debe lanzar DuplicateCategoryException cuando se intenta crear una categoría
     * cuyo nombre ya existe (entre los 5 registros existentes simulados).
     */
    @Test
    @DisplayName("createCategory - Lanzar DuplicateCategoryException por categoría duplicada")
    public void testCreateCategory_Duplicate() {
        // Arrange
        // Utilizamos un nombre ya existente en nuestro dataset simulado ("Salud")
        String duplicateName = category1.getName();
        CategoryRequest request = new CategoryRequest(duplicateName, "Otra descripción");

        // Simular que el repositorio indica que existe una categoría con ese nombre.
        when(categoryRepository.existsByName(duplicateName)).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateCategoryException.class,
                () -> categoryService.createCategory(request),
                "Se esperaba lanzar DuplicateCategoryException por categoría duplicada.");
        verify(categoryRepository, times(1)).existsByName(duplicateName);
        verify(categoryMapper, never()).toEntity(any());
        verify(categoryRepository, never()).save(any());
    }

    /**
     * Caso de error adicional: Simulación de excepción en la capa de persistencia al intentar guardar la categoría.
     */
    @Test
    @DisplayName("createCategory - Manejo de error en el repositorio durante el guardado")
    public void testCreateCategory_RepositoryError() {
        // Arrange
        CategoryRequest request = new CategoryRequest("Innovación", "Descripción de Innovación");
        when(categoryRepository.existsByName(request.name())).thenReturn(false);

        // Simular mapeo a entidad
        Category newCategory = createActiveCategory(request.name());
        when(categoryMapper.toEntity(request)).thenReturn(newCategory);

        // Simular que el repositorio lanza una excepción al intentar guardar la nueva entidad.
        when(categoryRepository.save(newCategory)).thenThrow(new RuntimeException("Error al guardar la categoría"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> categoryService.createCategory(request),
                "Se esperaba lanzar una excepción al ocurrir un error en el repositorio.");

        assertEquals("Error al guardar la categoría", exception.getMessage());
        verify(categoryRepository, times(1)).existsByName(request.name());
        verify(categoryRepository, times(1)).save(newCategory);
        verify(categoryMapper, times(1)).toEntity(request);
        verify(categoryMapper, never()).toCategoryResponse(any());
    }

    /**
     * Caso positivo: Actualizar exitosamente una categoría existente.
     * Se verifica que, dado un ID válido y una solicitud de actualización sin duplicidad,
     * se actualice la categoría correctamente.
     */
    @Test
    @DisplayName("updateCategory - Flujo positivo: Actualización exitosa")
    public void testUpdateCategory_Positive() {
        // Arrange
        // Seleccionamos category3 ("Salud") para actualizar
        Category existingCategory = category3;
        String validId = existingCategory.getId().toHexString();

        // Se crea un request con datos nuevos
        CategoryRequest request = new CategoryRequest("Salud y Bienestar", "Actualización de Salud");

        // Simular que se encontró la categoría en el repositorio
        when(categoryRepository.findByIdAndActivatedTrue(existingCategory.getId()))
                .thenReturn(Optional.of(existingCategory));
        // Simular que no existe otra categoría con el nombre nuevo
        when(categoryRepository.existsByName(request.name())).thenReturn(false);

        // Simular la actualización de la entidad a través del mapper
        doAnswer(invocation -> {
            CategoryRequest req = invocation.getArgument(0);
            Category cat = invocation.getArgument(1);
            cat.setName(req.name());
            cat.setDescription(req.description());
            return null;
        }).when(categoryMapper).updateCategoryFromRequest(request, existingCategory);

        // Simular la persistencia de la categoría actualizada
        when(categoryRepository.save(existingCategory)).thenReturn(existingCategory);

        // Para la conversión a DTO, se usa thenAnswer para reflejar los valores actualizados
        when(categoryMapper.toCategoryResponse(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            return new CategoryResponse(
                    c.getId().toHexString(),
                    c.getName(),
                    c.getDescription(),
                    c.getCreatedAt().toString()
            );
        });

        // Act
        CategoryResponse result = categoryService.updateCategory(validId, request);

        // Assert
        assertNotNull(result, "El CategoryResponse no debe ser nulo.");
        assertEquals("Salud y Bienestar", result.name(), "El nombre debe haberse actualizado.");
        assertEquals("Actualización de Salud", result.description(), "La descripción debe haberse actualizado.");

        verify(categoryRepository, times(1)).findByIdAndActivatedTrue(existingCategory.getId());
        verify(categoryRepository, times(1)).existsByName(request.name());
        verify(categoryMapper, times(1)).updateCategoryFromRequest(request, existingCategory);
        verify(categoryRepository, times(1)).save(existingCategory);
        verify(categoryMapper, times(1)).toCategoryResponse(existingCategory);
    }

    /**
     * Caso de error: El ID proporcionado es inválido.
     * Se espera que se lance una IdInvalidException si el formato del ID no corresponde a un ObjectId válido.
     */
    @Test
    @DisplayName("updateCategory - Id inválido lanza IdInvalidException")
    public void testUpdateCategory_InvalidId() {
        // Arrange: Un ID con formato no válido
        String invalidId = "abc123";

        // Act & Assert
        assertThrows(IdInvalidException.class,
                () -> categoryService.updateCategory(invalidId, new CategoryRequest("Nuevo Nombre", "Nueva Descripción")),
                "Se esperaba IdInvalidException debido a un ID no válido.");

        verify(categoryRepository, never()).findByIdAndActivatedTrue(any());
        verify(categoryMapper, never()).updateCategoryFromRequest(any(), any());
    }

    /**
     * Caso de error: La categoría no se encuentra en la base de datos.
     * Se espera que se lance CategoryNotFoundException cuando no se encuentra la categoría.
     */
    @Test
    @DisplayName("updateCategory - Categoría no encontrada lanza CategoryNotFoundException")
    public void testUpdateCategory_NotFound() {
        // Arrange: Usamos un ID válido que no se encuentra en el repositorio
        Category nonExistentCategory = createActiveCategory("No existente");
        String validId = nonExistentCategory.getId().toHexString();

        when(categoryRepository.findByIdAndActivatedTrue(nonExistentCategory.getId()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                CategoryNotFoundException.class,
                () -> categoryService.updateCategory(validId, new CategoryRequest("Algún Nombre", "Alguna Descripción")),
                "Se esperaba CategoryNotFoundException al no encontrar la categoría."
        );

        verify(categoryRepository, times(1)).findByIdAndActivatedTrue(nonExistentCategory.getId());
        verify(categoryRepository, never()).existsByName(any());
        verify(categoryMapper, never()).updateCategoryFromRequest(any(), any());
    }

    /**
     * Caso de error: Intento de actualización a un nombre ya existente en otra categoría.
     * Se espera que se lance DuplicateCategoryException.
     */
    @Test
    @DisplayName("updateCategory - Nombre duplicado lanza DuplicateCategoryException")
    public void testUpdateCategory_DuplicateName() {
        // Arrange: Seleccionamos una categoría existente, por ejemplo, category4 ("Educación")
        Category existingCategory = category4;
        String validId = existingCategory.getId().toHexString();

        // Creamos un request con un nombre distinto al actual pero que ya existe (por ejemplo, "Deportes")
        CategoryRequest request = new CategoryRequest("Deportes", "Actualización de Educación");

        // Simulamos que se encontró la categoría a actualizar
        when(categoryRepository.findByIdAndActivatedTrue(existingCategory.getId()))
                .thenReturn(Optional.of(existingCategory));
        // Simulamos que existe otra categoría con el nombre "Deportes"
        when(categoryRepository.existsByName(request.name())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateCategoryException.class,
                () -> categoryService.updateCategory(validId, request),
                "Se esperaba DuplicateCategoryException debido a nombre duplicado.");

        verify(categoryRepository, times(1)).findByIdAndActivatedTrue(existingCategory.getId());
        verify(categoryRepository, times(1)).existsByName(request.name());
        verify(categoryMapper, never()).updateCategoryFromRequest(any(), any());
        verify(categoryRepository, never()).save(any());
    }

    /**
     * Caso positivo: Desactivar exitosamente una categoría existente.
     * Se verifica que, dado un ID válido y que la categoría exista, ésta se marque como inactiva.
     */
    @Test
    @DisplayName("deactivateCategory - Flujo positivo: Desactivación exitosa")
    public void testDeactivateCategory_Positive() {
        // Arrange: Seleccionamos una categoría del dataset, por ejemplo category3 ("Salud")
        Category existingCategory = category3;
        String validId = existingCategory.getId().toHexString();

        // Simulamos que se encontró la categoría con el ID proporcionado.
        when(categoryRepository.findByIdAndActivatedTrue(existingCategory.getId()))
                .thenReturn(Optional.of(existingCategory));

        // Act: Invocamos el método a probar.
        categoryService.deactivateCategory(validId);

        // Assert: Verificamos que la categoría fue marcada como inactiva y se llamó al guardado.
        assertFalse(existingCategory.isActivated(), "La categoría debe estar marcada como inactiva.");
        verify(categoryRepository, times(1)).findByIdAndActivatedTrue(existingCategory.getId());
        verify(categoryRepository, times(1)).save(existingCategory);
    }

    /**
     * Caso de error: Si se proporciona un ID inválido se lanza IdInvalidException.
     */
    @Test
    @DisplayName("deactivateCategory - Id inválido lanza IdInvalidException")
    public void testDeactivateCategory_InvalidId() {
        // Arrange: Se utiliza un ID con formato incorrecto.
        String invalidId = "123abc";

        // Act & Assert: Se espera que se lance la excepción IdInvalidException y no se invoque al repositorio.
        assertThrows(IdInvalidException.class, () -> categoryService.deactivateCategory(invalidId),
                "Se esperaba lanzar IdInvalidException debido a un ID no válido.");
        verify(categoryRepository, never()).findByIdAndActivatedTrue(any());
        verify(categoryRepository, never()).save(any());
    }

    /**
     * Caso de error: Cuando no se encuentra la categoría activa se lanza CategoryNotFoundException.
     */
    @Test
    @DisplayName("deactivateCategory - Categoría no encontrada lanza CategoryNotFoundException")
    public void testDeactivateCategory_NotFound() {
        // Arrange: Usamos un ID válido, pero simulamos que el repositorio no encuentra la categoría.
        Category nonExistentCategory = createActiveCategory("No existe");
        String validId = nonExistentCategory.getId().toHexString();

        when(categoryRepository.findByIdAndActivatedTrue(nonExistentCategory.getId()))
                .thenReturn(Optional.empty());

        // Act & Assert: Se espera la excepción CategoryNotFoundException.
        assertThrows(CategoryNotFoundException.class,
                () -> categoryService.deactivateCategory(validId),
                "Se esperaba CategoryNotFoundException al no encontrar la categoría.");
        verify(categoryRepository, times(1)).findByIdAndActivatedTrue(nonExistentCategory.getId());
        verify(categoryRepository, never()).save(any());
    }

}
