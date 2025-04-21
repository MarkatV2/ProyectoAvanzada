package org.example.proyectoavanzada.repository;


import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.entity.category.Category;
import co.edu.uniquindio.proyecto.repository.CategoryRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@ContextConfiguration(classes = ProyectoApplication.class)
public class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    // Lista que contendrá un dataset con mínimo 5 categorías
    private List<Category> categories;

    @BeforeEach
    public void setUp() {
        // Limpiar la base de datos de pruebas para garantizar la repetibilidad
        mongoTemplate.getDb().listCollectionNames().forEach(mongoTemplate::dropCollection);

        categories = new ArrayList<>();

        // Creación de 5 instancias de Category con datos de ejemplo
        categories.add(createCategory("Tecnología", "Artículos sobre tecnología", true));
        categories.add(createCategory("Salud", "Contenido relacionado con la salud", true));
        categories.add(createCategory("Educación", "Recursos y artículos educativos", true));
        categories.add(createCategory("Entretenimiento", "Novedades de cine y música", true));
        categories.add(createCategory("Deportes", "Últimas noticias deportivas", true));

        // Guardar todas las categorías en la base de datos de pruebas
        categoryRepository.saveAll(categories);
    }

    /**
     * Método auxiliar para crear instancias de Category con datos básicos.
     */
    private Category createCategory(String name, String description, boolean activated) {
        Category category = new Category();
        // Se asume que el id se asigna automáticamente (por ejemplo, con ObjectId)
        category.setName(name);
        category.setDescription(description);
        category.setCreatedAt(LocalDateTime.now());
        category.setActivated(activated);
        return category;
    }

    @Test
    @DisplayName("Probar creación y consulta de todas las categorías")
    public void testFindAllCategories() {
        // Act: Recuperar todas las categorías de la base de datos
        List<Category> lista = categoryRepository.findAll();

        // Assert: Se debe encontrar 5 registros (el dataset)
        assertEquals(5, lista.size(), "Se esperaban 5 categorías en la base de datos.");
    }

    @Test
    @DisplayName("Probar la búsqueda de una categoría por ID")
    public void testFindCategoryById() {
        // Arrange: Tomar la primera categoría del dataset
        Category categoriaEsperada = categories.get(0);

        // Act: Buscar la categoría por su id
        Optional<Category> categoriaObtenida = categoryRepository.findById(categoriaEsperada.getId());

        // Assert: Se verifica que la categoría exista y que los datos sean correctos
        assertTrue(categoriaObtenida.isPresent(), "La categoría debería existir.");
        assertEquals(categoriaEsperada.getName(), categoriaObtenida.get().getName(), "Los nombres deben coincidir.");
    }

    @Test
    @DisplayName("Probar la actualización de una categoría")
    public void testUpdateCategory() {
        // Arrange: Obtener una categoría y modificar un atributo
        Category categoria = categories.get(1);
        String nuevoNombre = "Bienestar y Salud";
        categoria.setName(nuevoNombre);

        // Act: Guardar la actualización y recuperar la categoría actualizada
        categoryRepository.save(categoria);
        Optional<Category> categoriaActualizada = categoryRepository.findById(categoria.getId());

        // Assert: Verificar que la actualización se realizó correctamente
        assertTrue(categoriaActualizada.isPresent(), "La categoría actualizada debe existir.");
        assertEquals(nuevoNombre, categoriaActualizada.get().getName(), "El nombre actualizado debe coincidir.");
    }

    @Test
    @DisplayName("Probar la eliminación de una categoría")
    public void testDeleteCategory() {
        // Arrange: Seleccionar una categoría a eliminar
        Category categoria = categories.get(2);
        ObjectId idCategoria = categoria.getId();

        // Act: Eliminar la categoría y verificar que ya no se encuentre
        categoryRepository.delete(categoria);
        Optional<Category> categoriaEliminada = categoryRepository.findById(idCategoria);

        // Assert: La categoría no debe estar presente en la base de datos
        assertFalse(categoriaEliminada.isPresent(), "La categoría debería haber sido eliminada.");
    }
}

