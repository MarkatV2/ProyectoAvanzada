package org.example.proyectoavanzada.repository;

import co.edu.uniquindio.proyecto.ProyectoApplication;
import co.edu.uniquindio.proyecto.entity.image.Image;
import co.edu.uniquindio.proyecto.repository.ImageRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@ContextConfiguration(classes = ProyectoApplication.class)
class ImageRepositoryTest {

    @Autowired
    private ImageRepository imageRepository;

    private ObjectId reportId1;
    private ObjectId reportId2;

    @BeforeEach
    void setUp() {
        imageRepository.deleteAll();

        reportId1 = new ObjectId();
        reportId2 = new ObjectId();

        List<Image> imagenes = new ArrayList<>();

        // Imagen 1 asociada al reporte 1
        Image img1 = new Image();
        img1.setId(new ObjectId());
        img1.setUrl("http://imagen1.jpg");
        img1.setReportId(reportId1);

        // Imagen 2 asociada al reporte 1
        Image img2 = new Image();
        img2.setId(new ObjectId());
        img2.setUrl("http://imagen2.jpg");
        img2.setReportId(reportId1);

        // Imagen 3 asociada al reporte 2
        Image img3 = new Image();
        img3.setId(new ObjectId());
        img3.setUrl("http://imagen3.jpg");
        img3.setReportId(reportId2);

        imagenes.add(img1);
        imagenes.add(img2);
        imagenes.add(img3);

        imageRepository.saveAll(imagenes);
    }

    @Test
    @DisplayName("Obtener imágenes por reportId - caso estándar")
    void testFindAllByReportId_StandardCase() {
        // Act
        List<Image> result = imageRepository.findAllByReportId(reportId1);

        // Assert
        assertEquals(2, result.size(), "Se deben obtener 2 imágenes asociadas al reporte 1");
        assertTrue(result.stream().allMatch(img -> img.getReportId().equals(reportId1)), "Todas las imágenes deben pertenecer al reporte 1");
    }

    @Test
    @DisplayName("Obtener imágenes por reportId inexistente")
    void testFindAllByReportId_NotFound() {
        // Arrange
        ObjectId nonExistentReportId = new ObjectId();

        // Act
        List<Image> result = imageRepository.findAllByReportId(nonExistentReportId);

        // Assert
        assertNotNull(result, "La lista no debe ser nula");
        assertTrue(result.isEmpty(), "La lista debe estar vacía si no hay imágenes para el ID proporcionado");
    }

    @Test
    @DisplayName("Obtener imágenes por reportId con solo una imagen asociada")
    void testFindAllByReportId_SingleImage() {
        // Act
        List<Image> result = imageRepository.findAllByReportId(reportId2);

        // Assert
        assertEquals(1, result.size(), "Debe haber una única imagen asociada al reporte 2");
        assertEquals(reportId2, result.get(0).getReportId(), "La imagen debe pertenecer al reporte 2");
    }

    @Test
    @DisplayName("Obtener imágenes por reportId nulo")
    void testFindAllByReportId_Null() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            imageRepository.findAllByReportId(null);
        }, "Debe lanzar una excepción al pasar un ID nulo");
    }

}
